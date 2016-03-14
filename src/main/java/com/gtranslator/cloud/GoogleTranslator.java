package com.gtranslator.cloud;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtranslator.BaseException;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

class GoogleTranslator {

    public static class GoogleTranslatorResult {
        private Map<String, Map<String, Double>> translates = new HashMap<>();
        private String original;

        private GoogleTranslatorResult() {
        }

        public Map<String, Map<String, Double>> getTranslates() {
            return translates;
        }

        public String getOriginal() {
            return original;
        }
    }

    private static String cookie;

    public static GoogleTranslatorResult translate(String source, Lang srcLang, Lang trgLang) throws NoSuchMethodException, ScriptException, IOException, IllegalAccessException, InstantiationException {
        initCookie();

        String enLetter = source;
        String[] tk = getTk(enLetter).substring(1).split("=");
        UrlBuilder params = new UrlBuilder("https://translate.google.com.ua/translate_a/single").put("client", "t")
                .put("sl", getGoogleLangName(srcLang)).put("tl", getGoogleLangName(trgLang)).put("hl", "ru").put("dt", "bd").put("dt", "ex").put("dt", "ld")
                .put("dt", "md").put("dt", "qca").put("dt", "rw").put("dt", "rm").put("dt", "ss").put("dt", "t")
                .put("dt", "at").put("ie", "UTF-8").put("oe", "UTF-8").put("source", "btn").put("srcrom", "1")
                .put("ssel", "0").put("tsel", "0").put("kc", "0").put(tk[0], tk[1]).put("q", enLetter);
        URL url = new URL(params.build());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(15000);
            conn.setDoOutput(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("accept-encoding", "deflate");
            conn.setRequestProperty("accept-language", "en-US,en;q=0.8");
            conn.setRequestProperty("referer", "https://translate.google.com.ua/?hl=uk");
            conn.setRequestProperty("authority", "translate.google.com.ua");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36");
            if (!StringUtils.isBlank(cookie)) {
                conn.setRequestProperty("Cookie", cookie);
            }
            conn.setUseCaches(true);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String decodedString;
                while ((decodedString = in.readLine()) != null) {
                    sb.append(decodedString);
                }
            }
            return convert(sb.toString());
        } finally {
            conn.disconnect();
        }
    }

    /*
        not necessary
        They do not affect.
        for reliability but you can be not used.
     */
    private static void initCookie() throws NoSuchMethodException, ScriptException, IOException, IllegalAccessException, InstantiationException {
        if (!StringUtils.isBlank(cookie)) {
            return;
        }

        UrlBuilder params = new UrlBuilder("https://translate.google.com.ua");
        URL url = new URL(params.build());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setDoOutput(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("accept-encoding", "deflate");
        conn.setRequestProperty("accept-language", "en-US,en;q=0.8");
        conn.setRequestProperty("referer", "https://translate.google.com.ua/?hl=uk");
        conn.setRequestProperty("authority", "translate.google.com.ua");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36");
        if (!StringUtils.isBlank(cookie)) {
            conn.setRequestProperty("Cookie", cookie);
        }
        conn.setUseCaches(false);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String decodedString;
            while ((decodedString = in.readLine()) != null) {
                sb.append(decodedString);
            }
        }

        cookie = Utils.extractCookie(conn);
    }

    private static String getTk(String a) throws ScriptException, NoSuchMethodException, FileNotFoundException {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        InputStreamReader reader = new InputStreamReader(
                GoogleTranslator.class.getClassLoader().getResourceAsStream("gt.js"));
        engine.eval(reader);
        Invocable inv = (Invocable) engine;
        return inv.invokeFunction("tk", a).toString();
    }

    private static class UrlBuilder {
        private Map<String, List<String>> params = new LinkedHashMap<>();
        private String requestUrl;

        UrlBuilder(String requestUrl) {
            this.requestUrl = requestUrl;
        }

        UrlBuilder put(String key, Object value) {
            List<String> param = params.get(key);
            if (param == null) {
                param = new ArrayList<>();
                params.put(key, param);
            }
            param.add(value.toString());
            return this;
        }

        String build() throws UnsupportedEncodingException {
            StringBuilder sb = new StringBuilder(requestUrl);
            sb.append("?");
            for (Entry<String, List<String>> ent : params.entrySet()) {
                for (String value : ent.getValue()) {
                    sb.append(URLEncoder.encode(ent.getKey(), "UTF-8"));
                    sb.append("=");
                    sb.append(URLEncoder.encode(value, "UTF-8"));
                    sb.append("&");
                }
            }
            if (params.size() > 0) {
                sb.delete(sb.length() - 1, sb.length());
            }
            return sb.toString();
        }
    }

    private static String getGoogleLangName(Lang lang) {
        switch (lang.name()) {
            case "EN":
            case "RU":
                return lang.name().toLowerCase();
            case "UA":
                return "uk";
            default:
                throw new BaseException("not use lang");
        }
    }

    @SuppressWarnings("unchecked")
    static GoogleTranslatorResult convert(String origTranslatedText) throws InstantiationException, IllegalAccessException {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        List<List<List>> rt = gson.fromJson(origTranslatedText, List.class);
        String translatedText = "";
        Iterable<List> it = rt.get(0);
        for (List l : it) {
            if (l.get(0) instanceof String && !StringUtils.isBlank(l.get(0).toString())) {
                translatedText += l.get(0).toString();
            } else {
                break;
            }
        }
        Double weight = Double.valueOf(
                ObjectUtils.defaultIfNull(Utils.getObjectBySafePosition(rt, 0, 0, 4), "1.0").toString());
        GoogleTranslatorResult result = new GoogleTranslatorResult();
        result.original = origTranslatedText;
        Utils.getMultiplicityValueFromMap(result.translates, "", HashMap::new).put(translatedText, weight);
        if (rt.size() > 1 && rt.get(1) != null) {
            for (List cat : rt.get(1)) {
                for (List t : ((List<List>) cat.get(2))) {
                    Utils.getMultiplicityValueFromMap(result.translates, cat.get(0).toString(), HashMap::new).put(t.get(0).toString(), t.size() > 2 ? (Double) t.get(3) : 1.0);
                }
            }
        }
        return result;
    }
}