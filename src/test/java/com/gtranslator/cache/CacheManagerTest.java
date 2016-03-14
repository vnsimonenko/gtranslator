package com.gtranslator.cache;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.verification.Times;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CacheManagerTest implements ICacheManager {
    /*
    @Test
    public void testDictionaryService() throws Exception {
        String cn = DictionaryServiceImpl.class.getName();
        Class c = CacheManager.transform(cn);
        c.newInstance();
        Constructor cc = c.getConstructor(DictionaryRepository.class);
        DictionaryService dictionaryService = (DictionaryService) cc.newInstance(null, null);
    }
    */

    private Tester tester;

    public static class Tester {
        public void count() {
        }
    }

    public void setTester(Object tester) {
        this.tester = (Tester) tester;
    }

    @Test
    public void testInjectCacheToMethod() throws Exception {
        Class c = CacheManager.transform(CacheManagerTest.class.getName());
        Tester tester = mock(Tester.class);
        ICacheManager inst = (ICacheManager) c.newInstance();
        //c.getMethod("setTester", new Class<?>[]{Object.class}).setAccessible(true);
        inst.setTester(tester);
        Method evictMethod = c.getMethod("evict", new Class<?>[]{String.class, Integer.class});
        Method getMethod = c.getMethod("get", new Class<?>[]{String.class, Integer.class});

        getMethod.invoke(inst, new Object[]{"test", 1});
        verify(tester, new Times(1)).count();
        evictMethod.invoke(inst, new Object[]{"test", 2});
        verify(tester, new Times(2)).count();
        getMethod.invoke(inst, new Object[]{"test", 1});
        verify(tester, new Times(3)).count();
        getMethod.invoke(inst, new Object[]{"test", 1});
        Assert.assertEquals("test-1", getMethod.invoke(inst, new Object[]{"test", 1}));
        verify(tester, new Times(3)).count();
    }

    @Caching(name = "dictionary", key = "#p0", operType = Caching.TYPE.EVICT)
    public void evict(String arg1, Integer arg2) {
        tester.count();
    }

    @Caching(name = "dictionary", key = "#p0", operType = Caching.TYPE.GET)
    public String get(String arg1, Integer arg2) {
        tester.count();
        return "" + arg1 + "-" + arg2;
    }
}
