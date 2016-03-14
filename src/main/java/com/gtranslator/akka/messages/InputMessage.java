package com.gtranslator.akka.messages;

import com.gtranslator.storage.domain.Lang;

import java.io.Serializable;

public class InputMessage implements Serializable {
    private String source;
    private Lang srcLang;
    private Lang trgLang;
    private MessageType messageType;

    public InputMessage(String source, Lang srcLang, Lang trgLang, MessageType messageType) {
        this.source = source;
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        this.messageType = messageType;
    }

    public InputMessage(String source, MessageType messageType) {
        this.source = source;
        this.messageType = messageType;
    }

    public String getSource() {
        return source;
    }

    public Lang getSrcLang() {
        return srcLang;
    }

    public Lang getTrgLang() {
        return trgLang;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
