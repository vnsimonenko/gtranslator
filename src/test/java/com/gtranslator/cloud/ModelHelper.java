package com.gtranslator.cloud;

import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.utils.Utils;
import org.mockito.invocation.InvocationOnMock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ModelHelper {
    private static String jsonText = "{\"source\":\"test\",\"lang\":\"EN\",\"transcriptions\":{\"AM\":[\"1\",\"2\"]},\"translations\":{\"RU\":{\"NOUN\":{\"тест\":1.0,\"тест\":1.0}}}}";

    public static DictionaryModel dynAnswer(InvocationOnMock invocationOnMock) throws Throwable {
        String s = invocationOnMock.getArguments()[0].toString();
        Lang sl = invocationOnMock.getArguments().length > 1 ? (Lang) invocationOnMock.getArguments()[1] : null;
        Lang tl = invocationOnMock.getArguments().length > 2 ? (Lang) invocationOnMock.getArguments()[2] : null;
        return createDynamicModel(s, sl == null ? Lang.EN : sl);
    }

    private static Map<List<Object>, DictionaryModel> modelMap = new ConcurrentHashMap<>();

    public static DictionaryModel createDynamicModel(String source, Lang srcLang) throws Throwable {
        DictionaryModel model = modelMap.get(Arrays.asList(source, srcLang));
        if (model != null) {
            return model;
        }
        if (Utils.isWord(source)) {
            model = modelMap.get(Arrays.asList(source, srcLang));
            if (model == null) {
                model = new DictionaryModel(srcLang, source);
                model.addTranslation(Lang.RU, "category1", source, 1.0d);
                model.addTranslation(Lang.RU, "category1", source + "2", 1.0d);
                model.addTranslation(Lang.RU, "category1", source + "3", 0.5d);
                model.addTranslation(Lang.RU, "category2", source + "4", 1.0d);
                model.addTranslation(Lang.RU, "category3", source + "5", 0.5d);
                model.addTranscription(Phonetic.AM, source + "_am");
                model.addTranscription(Phonetic.BR, source + "_br");
                modelMap.put(Arrays.asList(source, srcLang), model);
            }
            return model;
        } else {
            double weight = 1.0d;
            for (String word : source.split(" ")) {
                model = new DictionaryModel(srcLang, Utils.normalText(word));
                model.addTranslation(Lang.RU, "category1", word, weight);
                model.addTranscription(Phonetic.AM, word + "_am");
                model.addTranscription(Phonetic.BR, word + "_br");
                model.addOriginalTranslation(Lang.RU, jsonText);
                weight -= 0.1d;
                modelMap.put(Arrays.asList(Utils.normalText(word), srcLang), model);
            }
            model = new DictionaryModel(srcLang, source);
            model.addTranslation(Lang.RU, "", source, 1.0d);
            model.addOriginalTranslation(Lang.RU, jsonText);
            return model;
        }
    }

    public static Map<List, DictionaryModel> createModels() throws Throwable {
        Map<List, DictionaryModel> models = new HashMap<>();
        DictionaryModel model = new DictionaryModel(Lang.EN, "test1");
        model.addTranslation(Lang.RU, "c1", "тест1", 1.0d);
        model.addTranslation(Lang.RU, "c1", "тест2", 1.0d);
        model.addTranslation(Lang.RU, "c1", "тест3", 0.5d);
        model.addTranslation(Lang.RU, "c2", "тест4", 0.5d);
        model.addTranslation(Lang.RU, "c2", "тест5", 0.5d);
        model.addTranscription(Phonetic.AM, "test1_am");
        model.addTranscription(Phonetic.AM, "test2_am");
        model.addTranscription(Phonetic.BR, "test_br");
        models.put(Arrays.asList(Lang.EN, "test1"), model);

        model = new DictionaryModel(Lang.EN, "test2");
        model.addTranslation(Lang.RU, "c1", "тест21", 1.0d);
        model.addTranslation(Lang.RU, "c1", "тест22", 1.0d);
        model.addTranslation(Lang.RU, "c1", "тест23", 0.5d);
        model.addTranslation(Lang.RU, "c2", "тест24", 0.5d);
        model.addTranslation(Lang.RU, "c2", "тест25", 0.5d);
        model.addTranscription(Phonetic.AM, "test21_am");
        model.addTranscription(Phonetic.AM, "test22_am");
        model.addTranscription(Phonetic.BR, "test2_br");
        models.put(Arrays.asList(Lang.EN, "test2"), model);

        model = new DictionaryModel(Lang.EN, "test1 test2");
        model.addTranslation(Lang.RU, "c1", "тест1 тест2", 1.0d);
        models.put(Arrays.asList(Lang.EN, "test1 test2"), model);
        return models;
    }
}
