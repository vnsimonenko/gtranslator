package com.gtranslator.akka.configuration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.gtranslator.akka.actors.GoogleActor;
import com.gtranslator.akka.actors.RouterActor;
import com.gtranslator.akka.extension.ActorRefUtils;
import com.gtranslator.akka.extension.SpringExtension;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@Configuration
@Lazy
@ComponentScan(basePackages = {
        "com.gtranslator.akka.actors",
        "com.gtranslator.akka.extension"})
class AkkaConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SpringExtension springExtension;

    @Bean
    @DependsOn({"akkaConfig"})
    ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("AkkaTranslator", configuration());
        springExtension.initialize(applicationContext);
        return system;
    }

    @Bean
    @DependsOn({"actorSystem", "akkaConfig"})
    ActorRef routerActorRef() {
        return ActorRefUtils.createRef(RouterActor.class);
    }

    @Bean
    @DependsOn({"actorSystem", "akkaConfig"})
    ActorRef googleActorRef() {
        return ActorRefUtils.createRef(GoogleActor.class);
    }

    @Bean(name = "akkaConfig")
    Config configuration() {
        return ConfigFactory.load();
    }
}
