package com.gtranslator.akka.extension;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.gtranslator.utils.SpringApplicationContext;
import org.apache.commons.lang3.StringUtils;

public class ActorRefUtils {
    public static ActorRef getRef(Class<? extends UntypedActor> clazz) {
        return (ActorRef) SpringApplicationContext.getContext().getBean(normalRef(clazz));
    }

    public static ActorRef createRef(Class<? extends UntypedActor> clazz) {
        ActorSystem system = getActorSystem();
        SpringExtension springExtension = SpringApplicationContext.getContext().getBean(SpringExtension.class);
        return system.actorOf(springExtension.props(normal(clazz)));
    }

    public static ActorSystem getActorSystem() {
        return SpringApplicationContext.getContext().getBean(ActorSystem.class);
    }

    private static String normal(Class clazz) {
        String className = clazz.getSimpleName();
        return StringUtils.lowerCase(className.substring(0, 1)) + className.substring(1);
    }

    private static String normalRef(Class clazz) {
        return normal(clazz) + "Ref";
    }
}
