package com.gtranslator.storage.service;

import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface DictionaryService {
    DictionaryModel findDictionary(String source, Lang srcLang);
    Collection<DictionaryModel> saveDictionaries(Collection<DictionaryModel> models);
    DictionaryModel saveDictionary(DictionaryModel model);
    List<DictionaryModel> getDictionaryModelsByDate(Date prevDate, Date currentDate);
    void export(String fileName, boolean hasOriginal);
}
