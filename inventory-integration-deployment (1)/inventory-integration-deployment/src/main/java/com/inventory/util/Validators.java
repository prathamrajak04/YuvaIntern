package com.inventory.util;

/**
 * Central place for input validation.
 *
 * REFACTORING (DRY): in the Week 3 code the same "is it blank?", "is it negative?" checks were
 * copy-pasted into the Item constructor and several setters. They now live here once, and every
 * caller gets identical rules and identical error messages.
 */
public final class Validators {

    private Validators() {
        // utility class - no instances
    }

    /**
     * Returns the trimmed value, or throws if it is null, blank or contains a line break.
     *
     * <p>INTEGRATION CHANGE (Week 5): the CSV storage writes one item per line, so a line break
     * inside an id or name would corrupt the data file. The rule lives here, at the model level,
     * so it is enforced no matter which storage back-end is used.
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
        String trimmed = value.trim();
        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(fieldName + " must not contain line breaks");
        }
        return trimmed;
    }

    /** Returns the value, or throws if it is negative. Zero is allowed. */
    public static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    /** Returns the price, or throws if it is negative, NaN or infinite. */
    public static double requireValidPrice(double value) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Price must be a non-negative finite number");
        }
        return value;
    }
}
