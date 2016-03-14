package com.gtranslator.akka.messages;

import com.gtranslator.storage.domain.DictionaryModel;

import java.io.Serializable;

public class OutputMessage<T extends InputMessage> implements Serializable {
    private T input;
    private DictionaryModel model;

    public OutputMessage(T input, DictionaryModel model) {
        this.input = input;
        this.model = model;
    }

    public T getInput() {
        return input;
    }

    public DictionaryModel getModel() {
        return model;
    }
}
