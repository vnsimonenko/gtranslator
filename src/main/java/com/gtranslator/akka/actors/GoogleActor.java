package com.gtranslator.akka.actors;

import akka.actor.UntypedActor;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.RequiresMessageQueue;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.gtranslator.akka.messages.InputMessage;
import com.gtranslator.akka.messages.MessageType;
import com.gtranslator.akka.messages.OutputMessage;
import com.gtranslator.cloud.TranslateService;
import com.gtranslator.storage.domain.DictionaryModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GoogleActor extends UntypedActor implements RequiresMessageQueue<BoundedMessageQueueSemantics> {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "Cloud");

    @Autowired
    private TranslateService translateService;

    private long lastVisit;

    @Override
    public void onReceive(Object message) throws Exception {
        long diff = System.currentTimeMillis() - lastVisit;
        if (diff < 1000)
        synchronized (this) {
            try {
                Thread.sleep(diff);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            InputMessage inputMessage = message instanceof InputMessage ? (InputMessage) message : null;
            if (inputMessage == null) {
                log.error("Unable to handle message {}", message);
                unhandled(message);
            } else if (inputMessage.getMessageType() == MessageType.GOOGLE) {
                long start = System.currentTimeMillis();
                DictionaryModel model = translateService.translateByGoogle(inputMessage.getSource(), inputMessage.getSrcLang(), inputMessage.getTrgLang());
                log.info("runtime of google service is " + (System.currentTimeMillis() - start) + " [ms]");
                OutputMessage result = new OutputMessage(inputMessage, model);
                getSender().tell(result, getSelf());
            } else {
                log.error("Unable to handle message {}", message);
                unhandled(message);
            }
        } finally {
            lastVisit = System.currentTimeMillis();
        }
    }
}
