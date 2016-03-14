package com.gtranslator.storage.validator;

public interface Validator<T> {
    boolean isValid(T arg);
}
