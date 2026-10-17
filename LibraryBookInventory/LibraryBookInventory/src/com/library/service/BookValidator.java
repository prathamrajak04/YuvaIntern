package com.library.service;

import com.library.exception.InvalidInputException;
import java.time.Year;

/**
 * All input-validation rules in one place. Each method either returns the cleaned-up value
 * or throws {@link InvalidInputException} with a message that explains what to fix.
 *
 * <p>The user interface calls these methods while the user is typing (so a mistake only
 * repeats one question), and the service calls them again before saving (so the rules hold
 * even if some other front end is added later).</p>
 */
public final class BookValidator {

    /** Earliest accepted publication year (the printing press era; rejects typos like 15 or 0). */
    public static final int MIN_YEAR = 1000;
    /** Longest accepted title/author, protects the table layout and rejects pasted garbage. */
    public static final int MAX_TEXT_LENGTH = 200;

    private BookValidator() { }   // utility class: no instances

    /**
     * ISBN must be 10 or 13 digits. Hyphens and spaces are allowed while typing and removed
     * ("978-0-13-468599-1" becomes "9780134685991"). An ISBN-10 may end with the letter X.
     */
    public static String validateIsbn(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("ISBN cannot be empty.");
        }
        String isbn = raw.replace("-", "").replace(" ", "").toUpperCase();
        boolean ten = isbn.matches("\\d{9}[\\dX]");
        boolean thirteen = isbn.matches("\\d{13}");
        if (!ten && !thirteen) {
            throw new InvalidInputException(
                "ISBN must have 10 or 13 digits (hyphens allowed), for example 978-0-13-468599-1.");
        }
        return isbn;
    }

    /** Title must contain text and be at most {@value #MAX_TEXT_LENGTH} characters. */
    public static String validateTitle(String raw) {
        return validateText(raw, "Title");
    }

    /** Author must contain text and be at most {@value #MAX_TEXT_LENGTH} characters. */
    public static String validateAuthor(String raw) {
        return validateText(raw, "Author");
    }

    /** Year must be a whole number between {@value #MIN_YEAR} and the current year. */
    public static int validateYear(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("Publication year cannot be empty.");
        }
        int year;
        try {
            year = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            // Convert the technical exception into a friendly message.
            throw new InvalidInputException("Publication year must be a whole number, for example 2018.");
        }
        int currentYear = Year.now().getValue();
        if (year < MIN_YEAR || year > currentYear) {
            throw new InvalidInputException(
                "Publication year must be between " + MIN_YEAR + " and " + currentYear + ".");
        }
        return year;
    }

    /** Shared rule for title and author: trimmed, not blank, not too long. */
    private static String validateText(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
        String text = raw.trim();
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidInputException(fieldName + " is too long (maximum " + MAX_TEXT_LENGTH + " characters).");
        }
        return text;
    }
}
