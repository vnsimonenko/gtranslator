package com.gtranslator.storage.batch;

import com.gtranslator.Application;
import com.gtranslator.client.PopupWindow;
import com.gtranslator.storage.domain.*;
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

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, BatchRepositoryTest.class})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class BatchRepositoryTest {

    @AfterClass
    public static void tearDown() throws Exception {
        SpringApplicationContext.reset();
    }

    @Bean
    public PopupWindow popupWindow() {
        return mock(PopupWindow.class);
    }

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Before
    public void up() {
    }

    @Test
    public void testAdd_Get_Word() {
        Category c = new Category();
        c.setText("category1");
        categoryRepository.save(c);


        Word w1 = new Word();
        w1.setText("word1");
        w1.setLang(Lang.EN);
        wordRepository.save(w1);
        Transcription t = new Transcription();
        t.setText("transcription1");
        t.setPhonetic(Phonetic.AM);
        t.setWord(w1);
        transcriptionRepository.save(t);

        Word w2 = new Word();
        w2.setText("word2");
        w2.setLang(Lang.RU);
        wordRepository.save(w2);
        t = new Transcription();
        t.setText("transcription2");
        t.setPhonetic(Phonetic.AM);
        t.setWord(w2);
        transcriptionRepository.save(t);

        Translation tl = new Translation();
        tl.setSword(w1);
        tl.setTword(w2);
        tl.setWeight(BigDecimal.ONE);
        tl.setCategory(c);
        translationRepository.save(tl);

        Iterable<Word> ws = wordRepository.findAll();
        for (Word it : ws) {
            System.out.println(it);
        }

        w1 = wordRepository.findWithTranslations(w1.getText(), w1.getLang(), Lang.RU);
        Assert.assertEquals(1, w1.getTranslations().size());
        Assert.assertEquals("word2", w1.getTranslations().iterator().next().getTword().getText());
        Assert.assertEquals(Lang.RU, w1.getTranslations().iterator().next().getTword().getLang());
        Assert.assertEquals(BigInteger.ONE, w1.getTranslations().iterator().next().getWeight().toBigInteger());
        Assert.assertEquals(Lang.EN, w1.getLang());

        w1 = wordRepository.findWithTranslations(w1.getText(), w1.getLang(), Lang.EN);
        Assert.assertNull(w1);

    }
}