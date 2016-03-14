package com.gtranslator.utils;

import com.gtranslator.BaseException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        if (context != null) {
            throw new BaseException("Repeated set of context");
        }
        context = ctx;
    }

    public static void reset() {
        context = null;
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
