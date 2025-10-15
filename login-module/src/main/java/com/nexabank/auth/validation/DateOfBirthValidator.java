package com.nexabank.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateOfBirthValidator implements ConstraintValidator<ValidDateOfBirth, String> {
    
    @Override
    public void initialize(ValidDateOfBirth constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) {
            return true; // Let @NotBlank handle null/empty validation
        }
        
        try {
            // Parse the date
            LocalDate dob = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate today = LocalDate.now();
            
            // Check if date is not in the future
            if (dob.isAfter(today)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Date of birth cannot be in the future")
                       .addConstraintViolation();
                return false;
            }
            
            // Check if person is at least 18 years old
            Period age = Period.between(dob, today);
            if (age.getYears() < 18) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("You must be at least 18 years old to register")
                       .addConstraintViolation();
                return false;
            }
            
            // Check if person is not too old (reasonable upper limit)
            if (age.getYears() > 120) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Please enter a valid date of birth")
                       .addConstraintViolation();
                return false;
            }
            
            return true;
            
        } catch (DateTimeParseException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Date of birth must be in YYYY-MM-DD format and be a valid date")
                   .addConstraintViolation();
            return false;
        }
    }
}