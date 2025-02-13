package it.unipi.distribooked.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectIdValidator implements ConstraintValidator<ValidObjectId, String> {

    Logger logger = LoggerFactory.getLogger(ObjectIdValidator.class);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        logger.info("Validating object ID: {}", value);
        if (value == null) return false;
        return value.matches("^[0-9a-fA-F]{24}$");
    }
}
