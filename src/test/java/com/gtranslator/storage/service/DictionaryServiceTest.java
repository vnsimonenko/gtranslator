package com.gtranslator.storage.service;

import com.gtranslator.Application;
import com.gtranslator.cache.CacheManager;
import com.gtranslator.client.PopupWindow;
import com.gtranslator.storage.domain.Dictionary;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.utils.SpringApplicationContext;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.internal.verification.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.json.JsonObject;
import javax.json.JsonString;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, DictionaryServiceTest.class})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class DictionaryServiceTest {
    private static String jsonText = "{\"source\":\"test\",\"lang\":\"EN\",\"transcriptions\":{\"AM\":[\"1\",\"2\"]},\"translations\":{\"RU\":{\"NOUN\":{\"тест\":1.0,\"тест\":1.0}}}}";

    @AfterClass
    public static void tearDown() throws Exception {
        SpringApplicationContext.reset();
    }

    @Bean
    public PopupWindow popupWindow() {
        return mock(PopupWindow.class);
    }

    @Autowired
    DictionaryService dictionaryService;

    static DictionaryRepository repository;

    @Bean
    public DictionaryService dictionaryService(DictionaryRepository dictionaryRepository) throws Exception {
        repository = mock(DictionaryRepository.class);
        doAnswer(invocationOnMock -> {
            String s = invocationOnMock.getArguments()[0].toString();
            Lang l = (Lang) invocationOnMock.getArguments()[1];
            return dictionaryRepository.findBySourceAndLang(s, l);
        }).when(repository).findBySourceAndLang(anyString(), Matchers.eq(Lang.EN));
        when(repository.save(any(Dictionary.class))).thenAnswer(invocationOnMock -> {
            Dictionary d = (Dictionary) invocationOnMock.getArguments()[0];
            return dictionaryRepository.save(d);
        });

        Class c = CacheManager.transform(DictionaryServiceImpl.class.getName());
        Constructor cc = c.getConstructor(DictionaryRepository.class);
        cc.setAccessible(true);
        dictionaryService = (DictionaryService) cc.newInstance(repository);
        return dictionaryService;
    }

    @Test
    public void testSaveDictionary() throws InstantiationException, IllegalAccessException {
        DictionaryModel model = new DictionaryModel(Lang.EN, "test")
                .addTranscription(Phonetic.AM, "ph1")
                .addTranslation(Lang.UA, "NOUN", "тест", 1.0)
                .addOriginalTranslation(Lang.UA, jsonText);

        dictionaryService.saveDictionary(model);
        dictionaryService.findDictionary("test", Lang.EN);
        verify(repository, new Times(2)).findBySourceAndLang("test", Lang.EN);
        model = dictionaryService.findDictionary("test", Lang.EN);
        verify(repository, new Times(2)).findBySourceAndLang("test", Lang.EN);
        Set<String> test = new HashSet<>(Arrays.asList("ph1"));
        JsonObject translationOfDict = model.toJson().getJsonObject(DictionaryModel.Fields.TRANSCRIPTIONS.FIELD);
        translationOfDict.getJsonArray("AM").forEach(jsonValue -> test.remove(((JsonString) jsonValue).getString()));
        translationOfDict = model.toJson().getJsonObject(DictionaryModel.Fields.TRANSLATIONS.FIELD);
        Assert.assertEquals(new BigDecimal("1.0").toBigInteger(), translationOfDict.getJsonObject(Lang.UA.name())
                .getJsonObject("NOUN").getJsonNumber("тест").bigDecimalValue().toBigInteger());
        Assert.assertEquals(0, test.size());
    }
}
