package com.gtranslator.storage.batch;

import com.gtranslator.Application;
import com.gtranslator.client.PopupWindow;
import com.gtranslator.storage.domain.*;
import com.gtranslator.storage.service.DictionaryService;
import com.gtranslator.utils.SpringApplicationContext;
import com.gtranslator.utils.Utils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, BatchServiceTest.class, BatchConfiguration.class})
@EnableBatchProcessing
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class BatchServiceTest {
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
    private BatchService batchService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Test
    public void testExecute() throws Exception {
        List<DictionaryModel> modelList = new ArrayList<>();
        int i = 2;
        while (i-- > 0) {
            DictionaryModel model = new DictionaryModel(Lang.EN, "test" + i)
                    .addTranscription(Phonetic.AM, "ph1")
                    .addTranslation(Lang.UA, "NOUN", "тест", 1.0)
                    .addOriginalTranslation(Lang.UA, jsonText);

            modelList.add(model);
        }
        dictionaryService.saveDictionaries(modelList);
        batchService.execute();

        checkWord();
        checkTranscription1();
        checkTranslation();

        for (DictionaryModel dm : modelList) {
            if (dm.getSource().equals("test0")) {
                dm.addTranscription(Phonetic.AM, "ph12");
            } else {
                dm.addTranscription(Phonetic.AM, "ph22");
            }
        }
        dictionaryService.saveDictionaries(modelList);
        batchService.execute();

        checkWord();
        checkTranscription2();
        checkTranslation();
    }

    private void checkWord() {
        Set<Object> expectedValues = new HashSet<>();
        expectedValues.add(Utils.createKey("test0", Lang.EN));
        expectedValues.add(Utils.createKey("test1", Lang.EN));
        expectedValues.add(Utils.createKey("тест", Lang.UA));
        for (Word w : wordRepository.findAll()) {
            expectedValues.remove(Utils.createKey(w.getText(), w.getLang()));
        }
        Assert.assertEquals(0, expectedValues.size());
    }

    private void checkTranscription1() {
        Set<Object> expectedValues = new HashSet<>();
        expectedValues.add(Utils.createKey("ph1", Phonetic.AM, "test0", Lang.EN));
        expectedValues.add(Utils.createKey("ph1", Phonetic.AM, "test1", Lang.EN));
        for (Transcription t : transcriptionRepository.findAll()) {
            expectedValues.remove(Utils.createKey(t.getText(), t.getPhonetic(), t.getWord().getText(), t.getWord().getLang()));
        }
        Assert.assertEquals(0, expectedValues.size());
    }

    private void checkTranscription2() {
        Set<Object> expectedValues = new HashSet<>();
        expectedValues.add(Utils.createKey("ph1", Phonetic.AM, "test0", Lang.EN));
        expectedValues.add(Utils.createKey("ph1", Phonetic.AM, "test1", Lang.EN));
        expectedValues.add(Utils.createKey("ph12", Phonetic.AM, "test0", Lang.EN));
        expectedValues.add(Utils.createKey("ph22", Phonetic.AM, "test1", Lang.EN));
        for (Transcription t : transcriptionRepository.findAll()) {
            expectedValues.remove(Utils.createKey(t.getText(), t.getPhonetic(), t.getWord().getText(), t.getWord().getLang()));
        }
        Assert.assertEquals(0, expectedValues.size());
    }

    private void checkTranslation() {
        Set<Object> expectedValues = new HashSet<>();
        expectedValues.add(Utils.createKey("test0", Lang.EN, "тест", Lang.UA));
        expectedValues.add(Utils.createKey("test1", Lang.EN, "тест", Lang.UA));
        for (Translation t : translationRepository.findAll()) {
            expectedValues.remove(
                    Utils.createKey(
                            t.getSword().getText(), t.getSword().getLang(),
                            t.getTword().getText(), t.getTword().getLang()));
        }
        Assert.assertEquals(0, expectedValues.size());
    }
}