package com.gtranslator.cloud;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import com.gtranslator.BaseException;
import com.gtranslator.akka.actors.GoogleActor;
import com.gtranslator.akka.actors.RouterActor;
import com.gtranslator.akka.extension.ActorRefUtils;
import com.gtranslator.akka.messages.InputMessage;
import com.gtranslator.akka.messages.MessageType;
import com.gtranslator.akka.messages.OutputMessage;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.storage.service.DictionaryService;
import com.gtranslator.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import scala.concurrent.Future;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("translateService")
class TranslateServiceImpl implements TranslateService {
    final static Logger logger = LoggerFactory.getLogger(TranslateServiceImpl.class);
    @Autowired
    private DictionaryService dictionaryService;

    private int TIMEOUT = 60000;
    private ConcurrentHashMap<List<Object>, Boolean> loadingGoogle = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> loadingIvona = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> oxfordOxford = new ConcurrentHashMap<>();
    private OxfordReceiver oxfordReceiver;
    private IvonaReceiver ivonaReceiver;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public TranslateServiceImpl(@Value("${workspace}") String workspace) {
        oxfordReceiver = new OxfordReceiver(Paths.get(workspace, "oxford").toAbsolutePath().toString());
        ivonaReceiver = new IvonaReceiver(Paths.get(workspace, "ivona").toAbsolutePath().toString());
    }

    class ContextTranslateEvent {
        String source;
        Lang srcLang;
        Lang trgLang;
        Callback callback;

        public ContextTranslateEvent(String source, Lang srcLang, Lang trgLang, Callback callback) {
            this.source = source;
            this.srcLang = srcLang;
            this.trgLang = trgLang;
            this.callback = callback;
        }

        @Override
        public String toString() {
            return String.format("%s, %s, %s", source, srcLang, trgLang);
        }
    }

    @EventListener
    public void handleContextRefresh(ContextTranslateEvent event) {
        try {
            asyncTranslate(event.source, event.srcLang, event.trgLang, event.callback);
        } catch (NoSuchMethodException | ScriptException | IOException ex) {
            throw new BaseException(ex, "handleContextRefresh", event);
        }
    }

    @Override
    public void syncTranslate(String source, Lang srcLang, Lang trgLang, Callback callback) throws NoSuchMethodException, ScriptException, IOException {
        eventPublisher.publishEvent(new ContextTranslateEvent(source, srcLang, trgLang, callback));
    }

    @Override
    public void asyncTranslate(final String source, final Lang srcLang, final Lang trgLang, Callback callback) throws NoSuchMethodException, ScriptException, IOException {
        DictionaryModel model = dictionaryService.findDictionary(source, srcLang);
        if (model == null) {
            model = dictionaryService.saveDictionary(new DictionaryModel(srcLang, source));
        }

        ActorRef routerActor = ActorRefUtils.getRef(RouterActor.class);
        ActorRef googleActor = ActorRefUtils.getRef(GoogleActor.class);
        final ActorSystem system = ActorRefUtils.getActorSystem();
        boolean isWord = Utils.isWord(source);

        if (!loadingGoogle.getOrDefault(Arrays.asList(source, srcLang), false)) {
            logger.info("google: " + source + "," + srcLang);
            Future<Object> fut = Patterns.ask(googleActor, new InputMessage(source, srcLang, trgLang, MessageType.GOOGLE), TIMEOUT);
            fut.onComplete(new OnComplete<Object>() {
                public void onComplete(Throwable t, Object result) {
                    loadingGoogle.remove(Arrays.asList(source, srcLang));
                    if (t == null && result != null) {
                        OutputMessage outputMessage = (OutputMessage) result;
                        callback.onComplete(outputMessage.getModel(), trgLang);
                    } else {
                        logger.error("fail in google translated.", t);
                        callback.onFailure(new BaseException(t));
                    }
                }
            }, system.dispatcher());
        }

        if (isWord && Lang.EN == srcLang && !loadingIvona.getOrDefault(source, false)
                && (!ivonaReceiver.existsAudioFile(source, Phonetic.AM) || !ivonaReceiver.existsAudioFile(source, Phonetic.BR))) {
            loadingIvona.put(source, true);
            Future<Object> ivonaFut = Patterns.ask(routerActor, new InputMessage(source, MessageType.IVONA), TIMEOUT);
            ivonaFut.onComplete(new OnComplete<Object>() {
                public void onComplete(Throwable t, Object result) {
                    loadingIvona.remove(source);
                    if (t == null && result != null) {
                        OutputMessage outputMessage = (OutputMessage) result;
                        callback.onComplete(outputMessage.getModel(), trgLang);
                    } else {
                        logger.error("fail in ivona.", t);
                        callback.onFailure(new BaseException(t));
                    }
                }
            }, system.dispatcher());
        }

        if (isWord && Lang.EN == srcLang && !oxfordOxford.getOrDefault(source, false) && !model.hasTranscription()) {
            oxfordOxford.put(source, true);
            Future<Object> oxfordFut = Patterns.ask(routerActor, new InputMessage(source, MessageType.OXFORD), TIMEOUT);
            oxfordFut.onComplete(new OnComplete<Object>() {
                public void onComplete(Throwable t, Object result) {
                    oxfordOxford.remove(source);
                    if (t == null && result != null) {
                        OutputMessage outputMessage = (OutputMessage) result;
                        callback.onComplete(outputMessage.getModel(), trgLang);
                    } else {
                        logger.error("fail in oxford.", t);
                        callback.onFailure(new BaseException(t));
                    }
                }
            }, system.dispatcher());
        }
    }

    @Override
    public DictionaryModel translateByGoogle(String source, Lang srcLang, Lang trgLang) throws NoSuchMethodException, ScriptException, IOException, InstantiationException, IllegalAccessException {
        DictionaryModel model = dictionaryService.findDictionary(source, srcLang);
        if (!model.hasTranslation(trgLang)) {
            GoogleTranslator.GoogleTranslatorResult result = GoogleTranslator.translate(source, srcLang, trgLang);
            for (Map.Entry<String, Map<String, Double>> ent : result.getTranslates().entrySet()) {
                for (Map.Entry<String, Double> ent2 : ent.getValue().entrySet()) {
                    model.addTranslation(trgLang, ent.getKey(), ent2.getKey(), ent2.getValue());
                }
            }
            model.addOriginalTranslation(trgLang, result.getOriginal());
            model = dictionaryService.saveDictionary(model);
        }
        return model;
    }

    @Override
    public DictionaryModel downloadAudioFileByIvona(String source) throws IOException {
        try {
            ivonaReceiver.load(source);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return dictionaryService.findDictionary(source, Lang.EN);
    }

    @Override
    public DictionaryModel downloadAudioFileByOxford(String source) throws IOException, InstantiationException, IllegalAccessException {
        try {
            OxfordReceiver.OxfordReceiverResult result = oxfordReceiver.load(source);
            Map<String, DictionaryModel> modelMap = new HashMap<>();
            for (String word : result.getWords()) {
                DictionaryModel model = modelMap.containsKey(word) ? modelMap.get(word) : dictionaryService.findDictionary(word, Lang.EN);
                if (model == null) {
                    model = new DictionaryModel(Lang.EN, word);
                }
                modelMap.put(word, model);
                for (Map.Entry<Phonetic, Map<String, String>> ent : result.getTranscriptionAndAudioFilePath(word).entrySet()) {
                    for (Map.Entry<String, String> ent2 : ent.getValue().entrySet()) {
                        model.addTranscription(Phonetic.AM.equals(ent.getKey())
                                ? Phonetic.AM : Phonetic.BR, ent2.getKey());
                    }
                }
            }
            dictionaryService.saveDictionaries(modelMap.values());
        } catch (BaseException ex) {
            logger.error(ex.getMessage());
        }
        return dictionaryService.findDictionary(source, Lang.EN);
    }

    @Override
    public boolean existsAudioFile(DictionaryModel model, Phonetic phonetic) {
        return ivonaReceiver.existsAudioFile(model.getSource(), phonetic) || oxfordReceiver.existsAudioFile(model, phonetic);
    }

    @Override
    public File getAudioFile(DictionaryModel model, Phonetic phonetic) {
        File f = oxfordReceiver.getAudioFile(model, phonetic);
        if (f == null) {
            f = ivonaReceiver.getAudioFile(model.getSource(), phonetic);
        }
        return f;
    }
}
