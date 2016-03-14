package com.gtranslator.cloud;

import com.gtranslator.BaseException;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.utils.Utils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

class OxfordReceiver {
    private static Logger logger = Logger.getLogger(OxfordReceiver.class);
    private final static String USERAGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";
    private static final String REQUEST = "http://www.oxfordlearnersdictionaries.com/definition/english/%1$s_1?q=%1$s";
    private String soundDirectoryPath;

    public static class OxfordReceiverResult {
        private Map<String, Map<Phonetic, Map<String, String>>> phoneticByWord = new HashMap<>();

        private OxfordReceiverResult() {
        }

        public Set<String> getWords() {
            return phoneticByWord.keySet();
        }

        public Map<Phonetic, Map<String, String>> getTranscriptionAndAudioFilePath(String word) {
            return phoneticByWord.get(word);
        }

        void add(String word, Phonetic phonetic, String transcription, String audioFilePath) throws InstantiationException, IllegalAccessException {
            Utils.getMultiplicityValueFromMap(
                    Utils.getMultiplicityValueFromMap(phoneticByWord, word, HashMap::new),
                    phonetic, HashMap::new).put(transcription, audioFilePath);
        }

        @Override
        public String toString() {
            return "OxfordReceiverResult{" +
                    "phoneticByWord=" + phoneticByWord +
                    '}';
        }
    }

    public OxfordReceiver() {
        this.soundDirectoryPath = Paths.get(System.getProperty("user.home"), "oxford-english-sound").toAbsolutePath().toString();
    }

    public OxfordReceiver(String soundDirectoryPath) {
        this.soundDirectoryPath = soundDirectoryPath;
        logger.info("oxford path: " + soundDirectoryPath);
    }

    public OxfordReceiverResult load(String enWord) {
        try {
            return capture(enWord);
        } catch (IOException | URISyntaxException | InstantiationException | IllegalAccessException ex) {
            logger.error(ex);
            throw new BaseException(ex, "OxfordReceiver.load", enWord);
        }
    }

    private OxfordReceiverResult capture(String enWord) throws IOException, URISyntaxException, IllegalAccessException, InstantiationException {
        String request = String.format(REQUEST, enWord);
        Document doc = Jsoup.connect(request).timeout(30000).get();
        Elements elements = doc.select(
                "div[class=\"pron-link\"] a[href^=\"http://www.oxfordlearnersdictionaries.com/pronunciation/english/\"]");
        OxfordReceiverResult result = new OxfordReceiverResult();
        if (elements.size() == 0) {
            return result;
        }
        String phonRequest = elements.get(0).attr("href");
        Document phdoc = Jsoup.connect(phonRequest).timeout(3000).get();
        for (String[] phonView : new String[][]{{"NAmE", "us", Phonetic.AM.name()},
                {"BrE", "uk", Phonetic.BR.name()}}) {
            elements = phdoc.select("div[class=\"pron_row clear_fix\"]:has(span:contains(" + phonView[0]
                    + ")) div[class=\"pron_row__wrap1\"]:has(span:contains(" + phonView[0] + "))");
            for (Element el : elements) {
                Elements phElements = el
                        .select("span:contains(" + phonView[0] + ") + span[class=\"pron_phonetic\"]:has(pnc.wrap)");
                Elements sndElements = el
                        .select("div[class=\"sound audio_play_button pron-" + phonView[1] + " icon-audio\"]");
                String href = sndElements.get(0).attr("data-src-mp3");
                String word = getWordFromHref(href, Phonetic.AM.name().equals(phonView[2]));
                File dir = Paths.get(soundDirectoryPath, phonView[2]).toFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String transcription = "";
                try {
                    transcription = phElements.get(0).text().substring(1, phElements.get(0).text().length() - 1);
                } catch (IndexOutOfBoundsException ex) {
                    logger.error("fail in capture: " + enWord + ", url: " + request);
                }
                File f = new File(dir, Utils.encodeFileName(word + "_", transcription) + ".mp3");
                if (!f.exists()) {
                    loadFile(f, href);
                }
                result.add(word, Phonetic.valueOf(phonView[2]), transcription, f.exists() ? f.getAbsolutePath() : null);
            }
        }
        return result;
    }

    private String getWordFromHref(String href, boolean isAm) {
        String word = href.substring(href.lastIndexOf('/') + 1, href.length() - 4);
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile(String.format("^([^0-9]*)[_0-9]*%s[_0-9]*", isAm ? "_us" : "_gb"));
        Matcher matcher = pattern.matcher(word);
        if (matcher.find()) {
            word = matcher.group(1);
            while (word.endsWith("_")) {
                word = word.substring(0, word.length() - 1);
            }
        }
        return word.replace("_", "'");
    }

    private boolean loadFile(File file, String request) throws IOException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
        }
        URL url = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setDoOutput(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USERAGENT);
        conn.setUseCaches(true);
        try (InputStream in = conn.getInputStream()) {
            long size = Files.copy(in, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING);
            return size > 0;
        } catch (IOException ex) {
            logger.error(ex.getMessage() + "; URL: ".concat(request), ex);
            throw ex;
        }
    }

    public boolean existsAudioFile(DictionaryModel model, Phonetic phonetic) {
        return getAudioFile(model, phonetic) != null;
    }

    public File getAudioFile(DictionaryModel model, Phonetic phonetic) {
        File dir = Paths.get(soundDirectoryPath, phonetic.name()).toFile();
        try {
            for (DictionaryModel.TranscriptionRecord trn : model.getTranscriptionRecords()) {
                File f = new File(dir, Utils.encodeFileName(model.getSource() + "_", trn.getTranscription()) + ".mp3");
                if (f.exists()) {
                    return f;
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new BaseException(ex);
        }
        return null;
    }

    public static void main(String... args) {
        OxfordReceiver service = new OxfordReceiver();
        OxfordReceiverResult oxfordResults = service.load(args[0]);
        System.out.println(oxfordResults);
    }
}
