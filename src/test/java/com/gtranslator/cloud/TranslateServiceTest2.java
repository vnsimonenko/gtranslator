package com.gtranslator.cloud;

import com.gtranslator.Application;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.utils.SpringApplicationContext;
import core.ParallelComputer;
import org.junit.*;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.model.InitializationError;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * use only for testing client gui
 */
@Ignore
public class TranslateServiceTest2 {

    @Ignore
    @RunWith(SpringJUnit4ClassRunner.class)
    @SpringApplicationConfiguration(classes= {Application.class, SpringTest.class})
    @ComponentScan(basePackageClasses = { TranslateServiceTest2.SpringTest.class })
    public static class SpringTest {
        {
            System.out.println(java.awt.GraphicsEnvironment.isHeadless());
        }

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
        }

        @Bean(name = "translateService")
        public TranslateService translateService() throws NoSuchMethodException, ScriptException, IOException, IllegalAccessException, InstantiationException {
            TranslateService translateService = mock(TranslateService.class);
            doAnswer(invocationOnMock -> {
                DictionaryModel model = ModelHelper.dynAnswer(invocationOnMock);
                TranslateService.Callback callback = (TranslateService.Callback) invocationOnMock.getArguments()[3];
                callback.onComplete(model, Lang.RU);
                return null;
            }).when(translateService).syncTranslate(anyString(), Matchers.eq(Lang.EN), Matchers.eq(Lang.RU), any(TranslateService.Callback.class));
            when(translateService.translateByGoogle(anyString(), Matchers.eq(Lang.EN), Matchers.eq(Lang.RU))).thenAnswer(new Answer<DictionaryModel>() {
                @Override
                public DictionaryModel answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return ModelHelper.dynAnswer(invocationOnMock);
                }
            });
            when(translateService.downloadAudioFileByIvona(anyString())).thenAnswer(new Answer<DictionaryModel>() {
                @Override
                public DictionaryModel answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return ModelHelper.dynAnswer(invocationOnMock);
                }
            });
            when(translateService.downloadAudioFileByOxford(anyString())).thenAnswer(new Answer<DictionaryModel>() {
                @Override
                public DictionaryModel answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return ModelHelper.dynAnswer(invocationOnMock);
                }
            });
            return translateService;
        }

        @Test
        public void test() {
            Assert.assertEquals(1,1);
        }


        @AfterClass
        public static void down() {
            //ParallelComputer.send(true);
        }
    }

    @Test
    public void testMain() throws InterruptedException, NoSuchMethodException, IOException, IllegalAccessException, InstantiationException, ScriptException, InitializationError {
        ParallelComputer computer = new ParallelComputer(new Class[]{SpringTest.class}, (executorService, threads) -> {
            try {
                BlockingQueue queue = ParallelComputer.getQueue();
                Boolean b = (Boolean) queue.poll(200000, TimeUnit.MILLISECONDS);
                if (b != null && b) {
                    return true;
                }
            } catch (InterruptedException e) {
            }
            Thread thread = threads.iterator().next();
            thread.stop();
            return true;
        });
        JUnitCore.runClasses(computer, computer.getClasses());
    }
}
