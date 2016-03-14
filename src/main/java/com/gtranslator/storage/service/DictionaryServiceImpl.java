package com.gtranslator.storage.service;

import com.gtranslator.BaseException;
import com.gtranslator.cache.Caching;
import com.gtranslator.storage.domain.Dictionary;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import java.io.FileOutputStream;
import java.util.*;

@Component("dictionaryService")
class DictionaryServiceImpl implements DictionaryService {
    final static Logger logger = LoggerFactory.getLogger(DictionaryServiceImpl.class);

    private DictionaryRepository dictionaryRepository;

    @Autowired
    public DictionaryServiceImpl(DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Caching(name = "dictionary", key = "{#p0,#p1}", operType = Caching.TYPE.GET)
    public DictionaryModel findDictionary(String source, Lang srcLang) {
        Dictionary dictionary = dictionaryRepository.findBySourceAndLang(source, srcLang);
        return dictionary == null ? null : new DictionaryModel(dictionary);
    }

    @Transactional
    public Collection<DictionaryModel> saveDictionaries(Collection<DictionaryModel> models) {
        try {
            List<DictionaryModel> result = new ArrayList<>(models.size());
            for (DictionaryModel model : models) {
                result.add(saveDictionary(model));
            }
            return result;
        } catch (Exception ex) {
            throw new BaseException(ex);
        }
    }

    @Transactional
    @Caching(name = "dictionary", key = "{#p0.source,#p0.sourceLang}", operType = Caching.TYPE.EVICT)
    public DictionaryModel saveDictionary(DictionaryModel model) {
        try {
            DictionaryModel lastVesrionModel = findDictionary(model.getSource(), model.getSourceLang());
            if (lastVesrionModel == null) {
                lastVesrionModel = new DictionaryModel(model.getSourceLang(), model.getSource());
                lastVesrionModel.setDictionary(new Dictionary());
                lastVesrionModel.getDictionary().setSource(model.getSource());
                lastVesrionModel.getDictionary().setLang(model.getSourceLang());
            }
            for (DictionaryModel.TranslationRecord trn : model.getTranslationRecords()) {
                lastVesrionModel.addTranslation(trn.getLang(), trn.getCategory(), trn.getTranslation(), trn.getWeight());
                lastVesrionModel.addOriginalTranslation(trn.getLang(), trn.getOriginal());
            }
            for (DictionaryModel.TranscriptionRecord trn : model.getTranscriptionRecords()) {
                lastVesrionModel.addTranscription(trn.getPhonetic(), trn.getTranscription());
            }
            lastVesrionModel.getDictionary().setData(lastVesrionModel.toJson().toString());
            dictionaryRepository.save(lastVesrionModel.getDictionary());
            return lastVesrionModel;
        } catch (Exception ex) {
            throw new BaseException(ex);
        }
    }

    @Override
    public List<DictionaryModel> getDictionaryModelsByDate(Date prevDate, Date currentDate) {
        Set<Dictionary> dictionaries = dictionaryRepository.findByLastModifiedDate(prevDate, currentDate);
        List<DictionaryModel> models = new ArrayList<>();
        for (Dictionary dictionary : dictionaries) {
            logger.info("***** Dictionary *****");
            //logger.info(dictionary.toString());
            models.add(new DictionaryModel(dictionary));
        }
        return models;
    }

    @Override
    public void export(String fileName, boolean hasOriginal) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonArrayBuilder dicsBuilder = Json.createArrayBuilder();
        Iterable<Dictionary> dictionaries = dictionaryRepository.findAll();
        for (Dictionary dic : dictionaries) {
            DictionaryModel model = new DictionaryModel(dic);
            EnumSet<DictionaryModel.Fields> includes = EnumSet.of(DictionaryModel.Fields.SOURCE,
                    DictionaryModel.Fields.LANG,
                    DictionaryModel.Fields.TRANSCRIPTIONS,
                    DictionaryModel.Fields.TRANSLATIONS);
            if (hasOriginal) {
                includes.add(DictionaryModel.Fields.ORIGINAL_TRANSLATIONS);
            }
            dicsBuilder.add(model.toJson(includes));
        }
        builder.add("dictionary", dicsBuilder);
        JsonObject jsDic = builder.build();
        Map<String, Object> properties = new HashMap<>();
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            Json.createWriterFactory(properties).createWriter(out).writeObject(jsDic);
        } catch (Exception ex) {
            logger.error("fail for export: " + fileName, ex);
        }
    }
}