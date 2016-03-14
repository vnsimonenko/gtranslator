package com.gtranslator.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CacheResponse;
import java.net.URLConnection;
import java.util.*;

public class SimpleCacheResponse extends CacheResponse {
    private Map<String, List<String>> headers;
    private SimpleCacheRequest request;
    private Date expires;

    public SimpleCacheResponse(SimpleCacheRequest request, URLConnection uc)
            throws IOException {
        this.request = request;
        Map<String, List<String>> headers = new HashMap<>();
        for (int i = 0; ; i++) {
            String name = uc.getHeaderFieldKey(i);
            String value = uc.getHeaderField(i);
            if (value == null) {
                break;
            }
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<>(1);
                headers.put(name, values);
            }
            values.add(value);
        }
        long expiration = uc.getExpiration();
        if (expiration != 0) {
            this.expires = new Date(expiration);
        }
        this.headers = Collections.unmodifiableMap(headers);
    }

    public InputStream getBody() {
        return new ByteArrayInputStream(request.getData());
    }

    public Map<String, List<String>> getHeaders()
            throws IOException {
        return headers;
    }

    public boolean isExpired() {
        if (expires == null) {
            return false;
        } else {
            return expires.before(new Date());
        }
    }
}