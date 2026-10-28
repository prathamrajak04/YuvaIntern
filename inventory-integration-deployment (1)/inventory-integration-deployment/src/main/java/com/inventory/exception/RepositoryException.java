package com.inventory.exception;

/**
 * Thrown when the storage layer fails (unreadable, corrupt or unwritable data file).
 *
 * INTEGRATION NOTE: file I/O throws checked IOException, which would leak a storage detail into
 * the service and UI layers. The repository translates it into this unchecked exception so upper
 * layers only need to know "storage failed" and can show a friendly message.
 */
public class RepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
