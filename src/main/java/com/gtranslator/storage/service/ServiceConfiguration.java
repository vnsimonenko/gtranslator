package com.gtranslator.storage.service;

import com.gtranslator.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Configuration
public class ServiceConfiguration {

    @Bean
    public DictionaryService dictionaryService(DictionaryRepository dictionaryRepository) throws Exception {
        Class c = CacheManager.transform(DictionaryServiceImpl.class.getName());
        Constructor cc = c.getConstructor(DictionaryRepository.class);
        cc.setAccessible(true);
        DictionaryService dictionaryService = (DictionaryService) cc.newInstance(dictionaryRepository);
        return dictionaryService;
    }


    @Bean
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator() {
            public Object generate(Object target, Method method, Object... params) {
                return generateKey(params);
            }
        };
    }
}
