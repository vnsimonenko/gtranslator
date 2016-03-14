package com.gtranslator.cache;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Caching {
    enum TYPE {GET, EVICT}
    String name();
    String key();
    TYPE operType();
}
