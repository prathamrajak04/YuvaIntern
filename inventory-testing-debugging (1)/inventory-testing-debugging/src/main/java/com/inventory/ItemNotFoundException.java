package com.inventory;

/** Thrown when an operation refers to an item id that does not exist. */
public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String id) {
        super("Item not found: " + id);
    }
}
