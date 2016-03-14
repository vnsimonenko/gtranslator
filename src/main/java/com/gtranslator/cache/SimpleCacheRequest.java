package com.gtranslator.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.CacheRequest;

public class SimpleCacheRequest extends CacheRequest {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    public OutputStream getBody() throws IOException {
        return out;
    }

    public void abort() {
        out = null;
    }

    public byte[] getData() {
        return out == null ? null : out.toByteArray();
    }
}