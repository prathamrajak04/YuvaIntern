package com.library.repository;

import com.library.model.Book;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for books. The service layer talks to this interface only, so the storage
 * technology (an in-memory map today, a file or database tomorrow) can change without touching
 * any business logic.
 */
public interface BookRepository {

    /** Stores a new book. Returns false (and stores nothing) if the ISBN is already present. */
    boolean add(Book book);

    /** Finds a book by ISBN; the Optional is empty when there is no such book. */
    Optional<Book> findByIsbn(String isbn);

    /** Returns all books in the order they were added. The list is a copy; changing it has no effect. */
    List<Book> findAll();

    /** Replaces the stored book that has the same ISBN. Returns false if no such book exists. */
    boolean update(Book book);

    /** Removes the book with this ISBN. Returns false if no such book exists. */
    boolean delete(String isbn);
}
