package com.gtranslator.cache;

public interface ICacheManager {
    void evict(String arg1, Integer arg2);
    String get(String arg1, Integer arg2);
    void setTester(Object object);
}
