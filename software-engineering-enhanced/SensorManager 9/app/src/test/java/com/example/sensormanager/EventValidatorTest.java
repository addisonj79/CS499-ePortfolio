package com.example.sensormanager;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EventValidatorTest {

    @Test
    public void validateName_rejectsEmpty() {
        assertNotNull(EventValidator.validateName(""));
        assertNotNull(EventValidator.validateName("   "));
    }

    @Test
    public void validateName_rejectsTooLong() {
        String tooLong = "a".repeat(EventValidator.MAX_NAME_LENGTH + 1);
        assertNotNull(EventValidator.validateName(tooLong));
    }

    @Test
    public void validateName_acceptsReasonableName() {
        assertNull(EventValidator.validateName("Team Meeting"));
    }

    @Test
    public void validateDate_rejectsEmpty() {
        assertNotNull(EventValidator.validateDate(""));
    }

    @Test
    public void validateDate_rejectsWrongFormat() {
        assertNotNull(EventValidator.validateDate("12/25/2026"));
    }

    @Test
    public void validateDate_rejectsImpossibleDate() {
        assertNotNull(EventValidator.validateDate("2026-13-40"));
    }

    @Test
    public void validateDate_acceptsWellFormedDate() {
        assertNull(EventValidator.validateDate("2026-12-25"));
    }
}
