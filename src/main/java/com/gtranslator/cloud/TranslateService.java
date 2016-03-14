package com.gtranslator.cloud;

import com.gtranslator.BaseException;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;

public interface TranslateService {

//    interface Event {
//        DictionaryModel getModel();
//        Lang getTargetLang();
//        boolean isTranslation();
//    }

    interface Callback {
        void onComplete(DictionaryModel model, Lang trgLang);

        void onFailure(BaseException t);
    }

    void syncTranslate(String source, Lang srcLang, Lang trgSrc, Callback callback) throws NoSuchMethodException, ScriptException, IOException;

    void asyncTranslate(String source, Lang srcLang, Lang trgSrc, Callback callback) throws NoSuchMethodException, ScriptException, IOException;

    DictionaryModel translateByGoogle(String source, Lang srcLang, Lang trgSrc) throws NoSuchMethodException, ScriptException, IOException, InstantiationException, IllegalAccessException;

    DictionaryModel downloadAudioFileByIvona(String source) throws IOException;

    DictionaryModel downloadAudioFileByOxford(String source) throws IOException, InstantiationException, IllegalAccessException;

    boolean existsAudioFile(DictionaryModel model, Phonetic phonetic);

    File getAudioFile(DictionaryModel model, Phonetic phonetic);
}
