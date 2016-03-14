package com.gtranslator.storage.validator;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JsonValidatorTest {
    private String jsonText = "{\"source\":\"test\",\"lang\":\"EN\",\"transcriptions\":{\"AM\":[\"test\",\"1\"]},\"translations\":{\"RU\":{\"NOUN\":{\"тест\":1.0,\"контроль\":0.5}}}}";
    private String jsonTextErr = "{\"source\":\"test\",\"lang\":\"EN\",\"transcriptions\":{\"AM\":[\"test\",\"тест\"]},\"translations\":{\"RU\":{\"NOUN\":{\"1\":1.0,\"контроль\":0.5}}}}";
    private static String schemeText;

    @BeforeClass
    public static void up() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(JsonValidatorTest.class.getClassLoader().getResourceAsStream("dictionary-scheme2.json"), out);
        schemeText = new String(out.toByteArray(), "UTF-8");
    }

    @Test
    public void testIsJsonValid() throws IOException, ProcessingException {
        Assert.isTrue(JsonUtils.isJsonValid(schemeText, jsonText));
    }

    @Test
    public void testModelIsJsonValid() throws IOException, ProcessingException, IllegalAccessException, InstantiationException {
        DictionaryModel dict = new DictionaryModel(Lang.EN, "test")
                .addTranscription(Phonetic.AM, "test")
                .addTranscription(Phonetic.AM, "тест")
                .addTranslation(Lang.RU, "NOUN", "тест", 1.0)
                .addTranslation(Lang.RU, "NOUN", "test", 0.5)
                .addOriginalTranslation(Lang.UA, jsonText);

        dict.toJson(DictionaryModel.Fields.TRANSLATIONS).toString();
        String text = dict.toJson().toString();
        Assert.isTrue(text.indexOf("test") != -1);
        Assert.isTrue(text.indexOf("AM") != -1);
        Assert.isTrue(text.indexOf("RU") != -1);
        Assert.isTrue(text.indexOf("NOUN") != -1);
        Assert.isTrue(text.indexOf("тест") != -1);
        Assert.isTrue(JsonUtils.isJsonValid(schemeText, text));
    }

    @Test
    public void testModelIsJsonFail() throws IOException, ProcessingException, IllegalAccessException, InstantiationException {
        Assert.isTrue(!JsonUtils.isJsonValid(schemeText, jsonTextErr));
    }
}
