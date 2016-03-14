package com.gtranslator.cloud;

import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.utils.Utils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class IvonaReceiver {

    private static Logger logger = Logger.getLogger(OxfordReceiver.class);
    private String soundDirectoryPath;
    private final static String USERAGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";

    public IvonaReceiver() {
        this.soundDirectoryPath = Paths.get(System.getProperty("user.home"), "ivona-english-audio").toAbsolutePath()
                .toString();
    }

    public IvonaReceiver(String soundDirectoryPath) {
        this.soundDirectoryPath = soundDirectoryPath;
        logger.info("ivona path: " + soundDirectoryPath);
    }

    public static class IvonaReceiverResult {
        public final String amFilePath;
        public final String brFilePath;

        private IvonaReceiverResult(String amFilePath, String brFilePath) {
            this.amFilePath = amFilePath;
            this.brFilePath = brFilePath;
        }

        @Override
        public String toString() {
            return "IvonaReceiverResult [amFilePath=" + amFilePath + ", brFilePath=" + brFilePath + "]";
        }
    }

    /**
     * Load mp3 file from cloud of ivona web resource
     *
     * @param source --
     * @return IvonaReceiverResult
     * @throws IOException
     */
    public IvonaReceiverResult load(String source) throws IOException {
        Map<String, String> result = capture(source);
        return new IvonaReceiverResult(result.get(Phonetic.AM.name()), result.get(Phonetic.BR.name()));
    }

    public boolean existsAudioFile(String engWord, Phonetic phonetic) {
        return getAudioFile(engWord, phonetic) != null;
    }

    public File getAudioFile(String engWord, Phonetic phonetic) {
        File f = Paths.get(soundDirectoryPath, phonetic.name(), engWord + ".mp3").toFile();
        return f.exists() ? f : null;
    }

    private Map<String, String> capture(String enWord) throws IOException {
        URL url = new URL("https://www.ivona.com/us/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USERAGENT);
        conn.setUseCaches(false);
        conn.setRequestProperty("Accept", "*/*");

        Document doc;
        try (InputStream in = conn.getInputStream()) {
            doc = Jsoup.parse(in, "UTF-8", "https://www.ivona.com");
        }
        String csrfield = doc.select("input[id=\"VoiceTesterForm_csrfield\"][name=\"csrfield\"][type=\"hidden\"]")
                .get(0).attr("value");

        String cookie = Utils.extractCookie(conn);

        Map<String, String> sndPaths = new HashMap<>();
        for (String[] phon : new String[][]{{Phonetic.AM.name(), "11"}, {Phonetic.BR.name(), "8"}}) {
            url = new URL("https://www.ivona.com/let-it-speak/?setLang=us");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", USERAGENT);
            conn.setUseCaches(false);
            conn.setRequestProperty("Cookie", cookie);
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");

            StringBuilder params = new StringBuilder();
            params.append("ext=mp3");
            params.append("&voiceSelector=");
            params.append(phon[1]);
            params.append("&text=");
            params.append(enWord);
            params.append("&send=Play");
            params.append("&csrfield=");
            params.append(csrfield);
            params.append("&ref-form-name=VoiceTesterForm");

            try (OutputStream output = conn.getOutputStream()) {
                output.write(params.toString().getBytes("UTF-8"));
            }

            String request;
            try (InputStream in = conn.getInputStream()) {
                JsonObject jsonObject = Json.createReader(in).readObject();
                request = jsonObject.getString("script");
                request = request.substring("window['voiceDemo'].audioUpdate('".length(), request.length() - 2);
            }

            cookie = Utils.extractCookie(conn);

            url = new URL(request);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setDoInput(true);
            conn.setRequestProperty("User-Agent", USERAGENT);
            conn.setUseCaches(false);
            conn.setRequestProperty("Cookie", cookie);
            conn.setRequestProperty("X-Requested-With", "ShockwaveFlash/20.0.0.267");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");

            File dir = Paths.get(soundDirectoryPath, phon[0]).toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (InputStream in = conn.getInputStream()) {
                Path path = Paths.get(dir.getAbsolutePath(), enWord + ".mp3");
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                sndPaths.put(phon[0], path.toAbsolutePath().toString());
            }
        }
        return sndPaths;
    }

    public static void main(String... args) throws IOException {
        IvonaReceiver service = new IvonaReceiver();
        IvonaReceiverResult ivonaResult = service.load("string");
        System.out.println(ivonaResult);
    }
}
