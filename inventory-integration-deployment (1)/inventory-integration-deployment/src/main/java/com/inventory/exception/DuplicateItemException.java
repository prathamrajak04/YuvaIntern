package com.inventory.exception;

/** Thrown when adding an item whose id already exists. */
public class DuplicateItemException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DuplicateItemException(String id) {
        super("Item already exists: " + id);
    }
}
