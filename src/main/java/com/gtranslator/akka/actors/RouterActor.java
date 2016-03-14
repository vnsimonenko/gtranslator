package com.gtranslator.akka.actors;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.BalancingRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.gtranslator.akka.extension.ActorRefUtils;
import com.gtranslator.akka.messages.InputMessage;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RouterActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "Supervisor");

    private Router router;

    @Override
    public void preStart() throws Exception {

        log.info("Starting up");

        List<Routee> routees = new ArrayList<Routee>();
        for (int i = 0; i < 2; i++) {
            ActorRef actor = ActorRefUtils.createRef(CloudActor.class);
            getContext().watch(actor);
            routees.add(new ActorRefRoutee(actor));
        }
        router = new Router(new BalancingRoutingLogic(), routees);
        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        InputMessage inputMessage = message instanceof InputMessage ? (InputMessage) message : null;
        if (inputMessage != null) {
            router.route(message, getSender());
        } else if (message instanceof Terminated) {
            // Readd cloud actors if one failed
            log.error("Readd cloud actors if one failed", message);
            router = router.removeRoutee(((Terminated) message).actor());
            ActorRef actor = ActorRefUtils.createRef(CloudActor.class);
            getContext().watch(actor);
            router = router.addRoutee(new ActorRefRoutee(actor));
        } else {
            log.error("Unable to handle message {}", message);
            unhandled(message);
        }
    }

    @Override
    public void postStop() throws Exception {
        log.info("Shutting down");
        super.postStop();
    }
}