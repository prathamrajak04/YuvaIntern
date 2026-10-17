package com.library.exception;

/** A book with the same ISBN already exists. */
public class DuplicateBookException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public DuplicateBookException(String isbn) { super("A book with ISBN " + isbn + " already exists."); }
}
