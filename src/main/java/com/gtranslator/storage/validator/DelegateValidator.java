package com.gtranslator.storage.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DelegateValidator implements ConstraintValidator<ValidatedBy, Object> {

    private Validator validator;

    @Override
    public void initialize(ValidatedBy validated) {
        try {
            validator = validated.value().newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext constraintValidatorContext) {
        return validator.isValid(obj);
    }
}
