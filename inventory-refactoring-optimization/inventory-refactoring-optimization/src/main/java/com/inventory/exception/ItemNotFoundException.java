package com.inventory.exception;

/** Thrown when an operation refers to an item id that does not exist. */
public class ItemNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ItemNotFoundException(String id) {
        super("Item not found: " + id);
    }
}
