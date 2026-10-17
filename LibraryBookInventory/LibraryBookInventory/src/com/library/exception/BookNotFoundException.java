package com.library.exception;

/** No book with the given ISBN exists in the library. */
public class BookNotFoundException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public BookNotFoundException(String isbn) { super("No book found with ISBN " + isbn + "."); }
}
