package com.library.exception;

/** The user typed a value that breaks a validation rule (blank title, bad ISBN, impossible year ...). */
public class InvalidInputException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) { super(message); }
}
