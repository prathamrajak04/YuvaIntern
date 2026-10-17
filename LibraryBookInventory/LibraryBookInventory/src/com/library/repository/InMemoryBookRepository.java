package com.library.repository;

import com.library.model.Book;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps all books in memory, as the assignment requires.
 *
 * <p><b>Why a LinkedHashMap&lt;String, Book&gt;?</b></p>
 * <ul>
 *   <li>Map keyed by ISBN: add, find, update and delete by ISBN are O(1) on average, and the
 *       map automatically makes ISBNs unique.</li>
 *   <li>"Linked": remembers insertion order, so listing shows books in the order they were added.</li>
 * </ul>
 * Data is lost when the program exits (no file or database by design).
 */
public class InMemoryBookRepository implements BookRepository {

    /** The one and only data store. Private, so other classes cannot bypass the rules below. */
    private final Map<String, Book> books = new LinkedHashMap<>();

    @Override
    public boolean add(Book book) {
        // putIfAbsent returns null when the key was free and the book was stored.
        return books.putIfAbsent(book.getIsbn(), book) == null;
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.ofNullable(books.get(isbn));
    }

    @Override
    public List<Book> findAll() {
        // Return a copy so callers cannot modify our internal collection.
        return new ArrayList<>(books.values());
    }

    @Override
    public boolean update(Book book) {
        if (!books.containsKey(book.getIsbn())) {
            return false;                       // nothing to update
        }
        books.put(book.getIsbn(), book);        // same key: replaces value, keeps position
        return true;
    }

    @Override
    public boolean delete(String isbn) {
        return books.remove(isbn) != null;
    }
}
