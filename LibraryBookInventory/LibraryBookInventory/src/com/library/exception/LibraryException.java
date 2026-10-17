package com.library.exception;

/**
 * Base class for every error this application raises on purpose.
 *
 * <p>It is an <i>unchecked</i> exception (extends RuntimeException) so that the service and
 * repository methods stay free of "throws" clauses. The user interface catches this one type
 * in a single place and prints {@link #getMessage()} - the message is always written for the
 * end user, never for the programmer.</p>
 */
public class LibraryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LibraryException(String message) { super(message); }
}
