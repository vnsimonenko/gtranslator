package com.gtranslator.storage.service;

import com.gtranslator.Application;
import com.gtranslator.client.PopupWindow;
import com.gtranslator.storage.domain.Dictionary;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.utils.SpringApplicationContext;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.ConstraintViolationException;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, DictionaryRepositoryTest.class})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class DictionaryRepositoryTest {
    private static String jsonText = "{\"source\":\"test\",\"lang\":\"EN\",\"transcriptions\":{\"AM\":[\"1\",\"2\"]},\"translations\":{\"RU\":{\"NOUN\":{\"тест\":1.0,\"тест\":0.5}}}}";
    private static String jsonFailText = "{\"source\":\"test\",\"lang\":\"EN\",\"transcriptions\":{\"AM\":[\"1\",\"2\"]},\"translations\":{\"RU\":{\"NOUN\":{\"тест1\":1.0,\"тест1\":0.5}}}}";

    @AfterClass
    public static void tearDown() throws Exception {
        SpringApplicationContext.reset();
    }

    @Bean
    public PopupWindow popupWindow() {
        return mock(PopupWindow.class);
    }

    @Autowired
    private DictionaryRepository dictionaryRepository;

    static boolean isInit = false;

    @Before
    public void up() {
    }

    @Test
    public void testEmptyDictionary() {
        Dictionary d = new Dictionary();
        d.setSource("test");
        d.setLang(Lang.EN);
        dictionaryRepository.save(d);
        Assert.assertTrue(!d.isNew());
        d = dictionaryRepository.findBySourceAndLang("test", Lang.EN);
        Assert.assertNotNull(d);
    }

    @Test
    public void testDictionary() {
        Dictionary d = new Dictionary();
        d.setSource("test2");
        d.setLang(Lang.EN);
        d.setData(jsonText);
        dictionaryRepository.save(d);
        Assert.assertTrue(!d.isNew());
        d = dictionaryRepository.findBySourceAndLang("test2", Lang.EN);
        Assert.assertNotNull(d);
        Assert.assertTrue(d.getData().indexOf("\"test\"") > 0);
    }

    @Test
    public void testFailDictionary() {
        Dictionary d = new Dictionary();
        d.setSource("test3");
        d.setLang(Lang.EN);
        d.setData(jsonFailText);
        try {
            dictionaryRepository.save(d);
            Assert.fail();
        } catch (ConstraintViolationException ex) {
        }
    }
}