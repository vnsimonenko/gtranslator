package com.gtranslator.akka.actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.gtranslator.akka.messages.*;
import com.gtranslator.cloud.TranslateService;
import com.gtranslator.storage.domain.DictionaryModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CloudActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "Cloud");

    @Autowired
    private TranslateService translateService;

    @Override
    public void onReceive(Object message) throws Exception {
        InputMessage inputMessage = message instanceof InputMessage ? (InputMessage) message : null;
        if (inputMessage == null) {
            log.error("Unable to handle message {}", message);
            unhandled(message);
        } else if (inputMessage.getMessageType() == MessageType.IVONA) {
            long start = System.currentTimeMillis();
            DictionaryModel model = translateService.downloadAudioFileByIvona(inputMessage.getSource());
            log.info("runtime of ivona service is " + (System.currentTimeMillis() - start) + " [ms]");
            OutputMessage result = new OutputMessage(inputMessage, model);
            getSender().tell(result, getSelf());
        } else if (inputMessage.getMessageType() == MessageType.OXFORD) {
            long start = System.currentTimeMillis();
            DictionaryModel model = translateService.downloadAudioFileByOxford(inputMessage.getSource());
            log.info("runtime of oxford service is " + (System.currentTimeMillis() - start) + " [ms]");
            OutputMessage result = new OutputMessage(inputMessage, model);
            getSender().tell(result, getSelf());
        } else {
            log.error("Unable to handle message {}", message);
            unhandled(message);
        }
    }
}
