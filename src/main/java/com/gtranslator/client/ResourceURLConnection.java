package com.gtranslator.client;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ResourceURLConnection extends URLConnection {

    //resource:file:
    //resource:stream:

    private String mediaType;
    private InputStream inputStream;

    protected ResourceURLConnection(URL url) {
        super(url);
        mediaType = url.getProtocol();
        inputStream = getClass().getResourceAsStream(url.getPath());
    }

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public String getContentType() {
        return mediaType;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public int getContentLength() {
        return -1;
    }
}
