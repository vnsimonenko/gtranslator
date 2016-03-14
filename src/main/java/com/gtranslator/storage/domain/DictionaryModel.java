package com.gtranslator.storage.domain;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.gtranslator.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class DictionaryModel implements Externalizable {

    private static final long serialVersionUID = 1L;

    public enum Fields {
        TRANSCRIPTIONS("transcriptions"),
        TRANSLATIONS("translations"),
        ORIGINAL_TRANSLATIONS("originalTranslations"),
        SOURCE("source"),
        LANG("lang");

        Fields(String FIELD) {
            this.FIELD = FIELD;
        }

        public final String FIELD;
    }

    class DictionaryBean {
        @SerializedName("source")
        private volatile String source;
        @SerializedName("lang")
        private volatile Lang lang;
        @SerializedName("transcriptions")
        private Map<Phonetic, Set<String>> transcriptions = Collections.synchronizedMap(new HashMap<>());
        @SerializedName("translations")
        private Map<Lang, Map<String, Map<String, BigDecimal>>> translations = Collections.synchronizedMap(new HashMap<>());
        @SerializedName("original_translations")
        private Map<Lang, String> originalTranslations = Collections.synchronizedMap(new HashMap<>());
    }

    private DictionaryBean dictionaryBean = new DictionaryBean();
    private Dictionary dictionary;

    public DictionaryModel() {
    }

    public DictionaryModel(Lang lang, String source) {
        this.dictionaryBean.source = source;
        this.dictionaryBean.lang = lang;
    }

    public DictionaryModel(String json) {
        if (!StringUtils.isBlank(json)) {
            Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
            dictionaryBean = gson.fromJson(json, DictionaryBean.class);
            dictionaryBean.transcriptions = Collections.synchronizedMap(
                    ObjectUtils.defaultIfNull(dictionaryBean.transcriptions, new HashMap<>()));
            dictionaryBean.translations = Collections.synchronizedMap(
                    ObjectUtils.defaultIfNull(dictionaryBean.translations, new HashMap<>()));
            dictionaryBean.originalTranslations = Collections.synchronizedMap(
                    ObjectUtils.defaultIfNull(dictionaryBean.originalTranslations, new HashMap<>()));
        }
    }

    public DictionaryModel(Dictionary dictionary) {
        this(dictionary.getData());
        this.dictionary = dictionary;
    }

    public String getSource() {
        return dictionaryBean.source;
    }

    public Lang getSourceLang() {
        return dictionaryBean.lang;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public DictionaryModel addTranslation(Lang lang, String category, String value, Double weight) throws InstantiationException, IllegalAccessException {
        BigDecimal w = new BigDecimal(weight, new MathContext(2, RoundingMode.HALF_DOWN));
        return addTranslation(lang, category, value, w);
    }

    public DictionaryModel addTranslation(Lang lang, String category, String value, BigDecimal weight) throws InstantiationException, IllegalAccessException {
        synchronized (dictionaryBean.translations) {
            Map<String, Map<String, BigDecimal>> cats = Utils.getMultiplicityValueFromMap(dictionaryBean.translations, lang, HashMap::new);
            Utils.getMultiplicityValueFromMap(cats, category, HashMap::new).put(value, weight);
        }
        return this;
    }

    public DictionaryModel addOriginalTranslation(Lang lang, String originalTranslation) throws InstantiationException, IllegalAccessException {
        if (dictionaryBean.originalTranslations == null) {
            dictionaryBean.originalTranslations = Collections.synchronizedMap(new HashMap<>());
        }
        dictionaryBean.originalTranslations.put(lang, originalTranslation);
        return this;
    }

    public DictionaryModel addTranscription(Phonetic phonetic, String value) throws InstantiationException, IllegalAccessException {
        synchronized (dictionaryBean.transcriptions) {
            Utils.getMultiplicityValueFromMap(dictionaryBean.transcriptions, phonetic, HashSet::new).add(value);
        }
        return this;
    }

    public boolean hasTranslation(Lang lang) {
        return hasTranscription() && dictionaryBean.translations.containsKey(lang);
    }

    public boolean hasTranscription() {
        return dictionaryBean.transcriptions != null && dictionaryBean.transcriptions.size() > 0;
    }

    public List<TranslationRecord> getTranslationRecords() {
        List<TranslationRecord> result = new ArrayList<>();
        if (dictionaryBean != null && dictionaryBean.translations != null) {
            synchronized (dictionaryBean.translations) {
                for (Map.Entry<Lang, Map<String, Map<String, BigDecimal>>> lng : dictionaryBean.translations.entrySet()) {
                    for (Map.Entry<String, Map<String, BigDecimal>> ctg : lng.getValue().entrySet()) {
                        for (Map.Entry<String, BigDecimal> trn : ctg.getValue().entrySet()) {
                            String original = null;
                            if (dictionaryBean.originalTranslations != null) {
                                original = dictionaryBean.originalTranslations.get(lng.getKey());
                            }
                            result.add(new TranslationRecord(lng.getKey(), ctg.getKey(), trn.getKey(), trn.getValue(), original));
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<TranscriptionRecord> getTranscriptionRecords() {
        List<TranscriptionRecord> result = new ArrayList<>();
        if (dictionaryBean != null && dictionaryBean.transcriptions != null) {
            synchronized (dictionaryBean.transcriptions) {
                for (Map.Entry<Phonetic, Set<String>> phn : dictionaryBean.transcriptions.entrySet()) {
                    for (String trn : phn.getValue()) {
                        TranscriptionRecord transcription = new TranscriptionRecord(phn.getKey(), trn);
                        result.add(transcription);
                    }
                }
            }
        }
        return result;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public JsonObject toJson(EnumSet<Lang> includeLangs, Fields... includeFields) {
        EnumSet<Fields> fields = includeFields.length == 0 ? null : EnumSet.copyOf(Arrays.asList(includeFields));
        DictionaryBean copy = new DictionaryBean();
        copy.source = dictionaryBean.source;
        copy.lang = dictionaryBean.lang;
        copy.transcriptions = dictionaryBean.transcriptions;
        copy.translations = new HashMap<>();
        for (Lang l : includeLangs) {
            if (dictionaryBean.translations != null && fields.contains(Fields.TRANSLATIONS)) {
                copy.translations.put(l, dictionaryBean.translations.get(l));
            }
            if (dictionaryBean.originalTranslations != null && fields.contains(Fields.ORIGINAL_TRANSLATIONS)) {
                copy.originalTranslations.put(l, dictionaryBean.originalTranslations.get(l));
            }
        }
        return modelToJson(copy, fields);
    }

    public JsonObject toJson(EnumSet<Fields> includeFields) {
        return modelToJson(dictionaryBean, includeFields);
    }

    public JsonObject toJson(Fields... includeFields) {
        EnumSet<Fields> fields = includeFields.length == 0 ? null : EnumSet.copyOf(Arrays.asList(includeFields));
        return modelToJson(dictionaryBean, fields);
    }

    private JsonObject modelToJson(DictionaryBean dictionaryBean, EnumSet<Fields> includeFields) {
        final Set<String> includes = includeFields == null || includeFields.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(Collections2.transform(includeFields, new Function<Fields, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Fields fields) {
                return fields.FIELD;
            }
        }));
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return includes.size() > 0 && !includes.contains(fieldAttributes.getName());
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        String jsonRepresentation = gson.toJson(dictionaryBean);
        return Json.createReader(new StringReader(jsonRepresentation)).readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        byte[] bs = toJson().toString().getBytes("UTF-8");
        out.writeInt(bs.length);
        out.write(bs);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        byte[] bs = new byte[size];
        in.readFully(bs);
        String dictionaryBeanText = new String(bs, "UTF-8");
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        dictionaryBean = gson.fromJson(dictionaryBeanText, DictionaryBean.class);
        dictionaryBean.transcriptions = Collections.synchronizedMap(
                ObjectUtils.defaultIfNull(dictionaryBean.transcriptions, new HashMap<>()));
        dictionaryBean.translations = Collections.synchronizedMap(
                ObjectUtils.defaultIfNull(dictionaryBean.translations, new HashMap<>()));
        dictionaryBean.originalTranslations = Collections.synchronizedMap(
                ObjectUtils.defaultIfNull(dictionaryBean.originalTranslations, new HashMap<>()));
    }

    public class TranslationRecord {
        private Lang lang;
        private String category;
        private String translation;
        private BigDecimal weight;
        private String original;

        public TranslationRecord(Lang lang, String category, String translation, BigDecimal weight, String original) {
            this.lang = lang;
            this.category = category;
            this.translation = translation;
            this.weight = weight;
            this.original = original;
        }

        public Lang getLang() {
            return lang;
        }

        public String getCategory() {
            return category;
        }

        public String getTranslation() {
            return translation;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public String getOriginal() {
            return original;
        }
    }

    public class TranscriptionRecord {
        private Phonetic phonetic;
        private String transcription;

        public TranscriptionRecord(Phonetic phonetic, String transcription) {
            this.phonetic = phonetic;
            this.transcription = transcription;
        }

        public Phonetic getPhonetic() {
            return phonetic;
        }

        public String getTranscription() {
            return transcription;
        }

        @Override
        public String toString() {
            return "TranscriptionRecord{" +
                    "phonetic=" + phonetic +
                    ", transcription='" + transcription + '\'' +
                    '}';
        }
    }
}
