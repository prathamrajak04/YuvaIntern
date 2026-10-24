package com.inventory;

/** Thrown when adding an item whose id already exists. */
public class DuplicateItemException extends RuntimeException {
    public DuplicateItemException(String id) {
        super("Item already exists: " + id);
    }
}
