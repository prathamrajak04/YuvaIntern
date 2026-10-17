package com.library.model;

import java.util.Objects;

/**
 * The Book entity: one title held by the library.
 *
 * <p>The class is <b>immutable</b>: once created, its fields never change. To "update" a book
 * the service builds a new Book with the same ISBN and the repository replaces the old one.
 * Immutability makes the object safe to share between layers and prevents accidental edits.</p>
 *
 * <p>The ISBN is the book's unique identifier. Two Book objects with the same ISBN are
 * considered equal (see {@link #equals(Object)}).</p>
 */
public final class Book {

    private final String isbn;          // unique identifier, digits only (10 or 13 digits, ISBN-10 may end in X)
    private final String title;         // non-blank
    private final String author;        // non-blank
    private final int publicationYear;  // between 1000 and the current year

    /**
     * Creates a book. Values are assumed to be already validated by
     * {@link com.library.service.BookValidator}; this constructor only stores them.
     */
    public Book(String isbn, String title, String author, int publicationYear) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
    }

    public String getIsbn()         { return isbn; }
    public String getTitle()        { return title; }
    public String getAuthor()       { return author; }
    public int getPublicationYear() { return publicationYear; }

    /** Two books are the same book if their ISBNs match; other fields do not matter. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        return isbn.equals(((Book) o).isbn);
    }

    /** Must be consistent with equals(): based on the ISBN only. */
    @Override
    public int hashCode() { return Objects.hash(isbn); }

    /** Readable one-line description, used when a single book is shown to the user. */
    @Override
    public String toString() {
        return "\"" + title + "\" by " + author + " (" + publicationYear + "), ISBN " + isbn;
    }
}
