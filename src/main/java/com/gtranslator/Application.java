package com.gtranslator;

import akka.actor.ActorSystem;
import com.gtranslator.cache.MemoryCache;
import com.gtranslator.client.ClipboardObserver;
import com.gtranslator.utils.CliService;
import javafx.application.Platform;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.BeansException;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.*;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.ResponseCache;
import java.util.Arrays;

@EnableJpaRepositories
@EnableCaching
@SpringBootApplication
@EnableTransactionManagement
@EnableJpaAuditing
@EnableBatchProcessing
@EnableAspectJAutoProxy
/**
 * The entry point to the program
 */
public class Application implements CommandLineRunner, PropertyChangeListener, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * records events from different modules. observer.
     */
    public final static PropertySupport PROPERTYSUPPORT = new PropertySupport();

    private static ConfigurableApplicationContext context;

    /**
     * Checks the syntax of the command line to the correct input.
     * To enable or disable the web server.
     */
    private static CliService cliService = new CliService()
            .builder("m")
            .longOpt("mode")
            .argName("mode")
            .desc("client/server/ or is empty")
            .hasArg(true)
            .valueSeparator('=')
            .build()
            .builder("w")
            .longOpt("workspace")
            .argName("workspace")
            .hasArg(true)
            .desc("рабочая область проекта. настройки, audio files, db, log")
            .build()
            .builder("s")
            .longOpt("spring.output.ansi.enabled")
            .hasArg(true)
            .valueSeparator('=')
            .build();

    public static void main(String... args) throws ParseException, IOException, InterruptedException {
        logger.info("Application starting ...");
        CommandLine line = cliService.parse(args);
        boolean hasWebEnv = line.hasOption("mode") && line.getOptionValue("mode", "server").equals("server");
        SpringApplication app = new SpringApplication(Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebEnvironment(hasWebEnv);
        app.setHeadless(false);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Application.PROPERTYSUPPORT.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PropertySupport.Property.EXIT.equalsEvent(evt)) {
            Arrays.asList(PROPERTYSUPPORT.pcs.getPropertyChangeListeners())
                    .forEach(PROPERTYSUPPORT.pcs::removePropertyChangeListener);
            if (context.isActive()) {
                context.close();
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = (ConfigurableApplicationContext) applicationContext;
        context.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
            @Override
            public void onApplicationEvent(ContextClosedEvent event) {
                ClipboardObserver.INSTANCE.close();
                Platform.exit();
                ActorSystem system = context.getBean(ActorSystem.class);
                system.terminate();
            }
        });
    }

    public static class PropertySupport {
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public synchronized void firePropertyChange(Property property, Object oldValue,
                                                    Object newValue) {
            pcs.firePropertyChange(property.name(), oldValue, newValue);
        }

        public enum Property {
            SRC_LANG, TRG_LANG, AMOUNT_VIEW_WORDS, ACTIVE, EXIT, HISTORY,
            MODE, AM_AUTO_PLAY, BR_AUTO_PLAY;

            public boolean equalsEvent(PropertyChangeEvent evt) {
                return name().equals(evt.getPropertyName());
            }
        }
    }

    static {
        ResponseCache.setDefault(new MemoryCache());
    }
}
