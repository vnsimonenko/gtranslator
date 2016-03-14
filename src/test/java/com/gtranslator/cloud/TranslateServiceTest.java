package com.gtranslator.cloud;

import com.gtranslator.Application;
import com.gtranslator.BaseException;
import com.gtranslator.client.JsonTransformer;
import com.gtranslator.client.PopupWindow;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.utils.SpringApplicationContext;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, TranslateServiceTest.class})
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class TranslateServiceTest {

    @AfterClass
    public static void tearDown() throws Exception {
        SpringApplicationContext.reset();
    }

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public PopupWindow popupWindow() {
        return mock(PopupWindow.class);
    }

    @Bean(name = "translateService")
    public TranslateService translateService(AutowireCapableBeanFactory ctx, @Value("${workspace}") String propWorkspace) throws NoSuchMethodException, ScriptException, IOException, IllegalAccessException, InstantiationException {
        TranslateService translateService = new TranslateServiceImpl(propWorkspace);
        ctx.autowireBean(translateService);
        translateService = spy(translateService);
        doAnswer(invocationOnMock -> getModel(invocationOnMock)).when(translateService).translateByGoogle(anyString(), Matchers.eq(Lang.EN), Matchers.eq(Lang.RU));
        doAnswer(invocationOnMock -> getModel(invocationOnMock)).when(translateService).downloadAudioFileByIvona(anyString());
        doAnswer(invocationOnMock -> getModel(invocationOnMock)).when(translateService).downloadAudioFileByOxford(anyString());
        return translateService;
    }

    private DictionaryModel getModel(InvocationOnMock invocationOnMock) {
        String s = invocationOnMock.getArguments()[0].toString();
        Lang sl = invocationOnMock.getArguments().length > 1 ? (Lang) invocationOnMock.getArguments()[1] : null;
        return models.get(Arrays.asList(sl == null ? Lang.EN : sl, s));
    }

    private static Map<List, DictionaryModel> models;

    @Before
    public void setUp() throws Throwable {
        MockitoAnnotations.initMocks(this);
        models = ModelHelper.createModels();
    }

    @Test
    public void testSyncTranslates() throws InterruptedException, NoSuchMethodException, IOException, IllegalAccessException, InstantiationException, ScriptException {
        for (Map.Entry<List, DictionaryModel> ent : models.entrySet()) {
            DictionaryModel model = models.get(Arrays.asList((Lang) ent.getKey().get(0), ent.getKey().get(1).toString()));
            testSyncTranslate((Lang) ent.getKey().get(0), ent.getKey().get(1).toString(), model);
        }
    }

    public void testSyncTranslate(Lang srcLang, String source, DictionaryModel model) throws InterruptedException, NoSuchMethodException, IOException, IllegalAccessException, InstantiationException, ScriptException {
        TranslateService translateService = applicationContext.getBean(TranslateService.class);
        List<DictionaryModel> result = new ArrayList<>();
        CountDownLatch downLatch = new CountDownLatch(3);
        translateService.syncTranslate(source, srcLang, Lang.RU, new TranslateService.Callback() {
            @Override
            public void onComplete(DictionaryModel model, Lang trgLang) {
                downLatch.countDown();
                result.add(model);
            }
            @Override
            public void onFailure(BaseException t) {
            }
        });
        downLatch.await(60000, TimeUnit.MILLISECONDS);
        Assert.assertEquals(3, result.size());
        JsonTransformer transformer = JsonTransformer.createJsonTransformer();
        String expHtml = transformer.convertJsonToHtml(model.toJson().toString(), JsonTransformer.XSL.HTML);
        for (DictionaryModel test : result) {
            String actHtml = transformer.convertJsonToHtml(test.toJson().toString(), JsonTransformer.XSL.HTML);
            if (expHtml.equals(actHtml)) {
                return;
            }
        }
        Assert.fail();
    }
}
