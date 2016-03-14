package com.gtranslator.utils;

import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Utils {

    public interface ValueFactory<T> {
        T create();
    }

    public static <T, K1> T getMultiplicityValueFromMap(Map<K1, T> map, K1 key, ValueFactory<T> factory) throws IllegalAccessException, InstantiationException {
        T val = map.get(key);
        if (val == null) {
            val = factory.create();
            map.put(key, val);
            return val;
        }
        return val;
    }

    public static String extractCookie(URLConnection conn) {
        StringBuilder cookieSb = new StringBuilder();
        conn.getHeaderFields().entrySet().stream().filter(ent -> ent.getKey() != null && ent.getKey().equals("Set-Cookie")).forEach(ent -> {
            for (String s : ent.getValue()) {
                cookieSb.append(s);
                cookieSb.append(";");
            }
        });
        return cookieSb.toString();
    }

    /**
     * encode to base64
     * @param prefixASCII
     * @param decodedSuffixName
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String encodeFileName(String prefixASCII, String decodedSuffixName) throws UnsupportedEncodingException {
        return (prefixASCII == null ? "" : prefixASCII) + BaseEncoding.base64Url().omitPadding().encode(decodedSuffixName.getBytes("UTF-8")).toString();
    }

    /**
     * decode from base64
     * @param prefixASCII
     * @param codedSuffixName
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decodeFileName(String prefixASCII, String codedSuffixName) throws UnsupportedEncodingException {
        String prefix = prefixASCII == null ? "" : prefixASCII;
        byte[] b = BaseEncoding.base64Url().omitPadding().decode(codedSuffixName.substring(prefix.length()));
        return prefix + new String(b, "UTF-8");
    }

    public static String normalText(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }

        int left = 0;
        int right = text.length() - 1;
        while (left < text.length()) {
            if (Character.isLetter(text.charAt(left))) {
                break;
            } else {
                left++;
            }
        }
        while (right >= 0 && right >= left) {
            if (Character.isLetter(text.charAt(right))) {
                break;
            } else {
                right--;
            }
        }
        if (right == text.length()) {
            return StringUtils.EMPTY;
        }
        try {
            return text.substring(left, right + 1).replaceAll("[ ]+", " ").toLowerCase();
        } catch (StringIndexOutOfBoundsException ex) {
            return StringUtils.EMPTY;
        }
    }

    public static boolean isWord(String text) {
        return text.split(" ").length == 1;
    }

    public static List createKey(Object... attributes) {
        return Collections.unmodifiableList(Arrays.asList(attributes));
    }

    public static Object getObjectBySafePosition(Object objList, int... indexs) {
        if (!(objList instanceof List)) {
            return null;
        }
        Object result = objList;
        int pos = indexs.length;
        for (int ind : indexs) {
            if (((List)result).size() <= ind) {
                return null;
            }
            pos--;
            result = ((List)result).get(ind);
            if (!(result instanceof List)) {
                break;
            }
        }
        return pos == 0 ? result : null;
    }
}
