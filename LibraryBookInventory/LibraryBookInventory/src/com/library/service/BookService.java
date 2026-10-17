package com.library.service;

import com.library.exception.BookNotFoundException;
import com.library.exception.DuplicateBookException;
import com.library.model.Book;
import com.library.repository.BookRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The business logic for the four CRUD operations (plus search).
 *
 * <p>Flow of data: ConsoleMenu (text typed by the user) &rarr; BookService (validates, applies
 * rules, throws exceptions) &rarr; BookRepository (stores the data). The service does no
 * printing and no reading from the keyboard, which is what makes it easy to test.</p>
 */
public class BookService {

    private final BookRepository repository;

    /** The repository is passed in (dependency injection) instead of created here. */
    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    /**
     * CREATE: validates every field and stores a new book.
     *
     * @throws com.library.exception.InvalidInputException if a field breaks a rule
     * @throws DuplicateBookException                      if the ISBN is already in the library
     */
    public Book addBook(String isbn, String title, String author, String year) {
        Book book = new Book(
                BookValidator.validateIsbn(isbn),
                BookValidator.validateTitle(title),
                BookValidator.validateAuthor(author),
                BookValidator.validateYear(year));
        if (!repository.add(book)) {
            throw new DuplicateBookException(book.getIsbn());
        }
        return book;
    }

    /** READ (one): returns the book with this ISBN or throws BookNotFoundException. */
    public Book getBook(String isbn) {
        String clean = BookValidator.validateIsbn(isbn);
        return repository.findByIsbn(clean).orElseThrow(() -> new BookNotFoundException(clean));
    }

    /** READ (all): every book, sorted alphabetically by title (case-insensitive). */
    public List<Book> listBooks() {
        List<Book> all = repository.findAll();
        all.sort(Comparator.comparing(b -> b.getTitle().toLowerCase(Locale.ROOT)));
        return all;
    }

    /** READ (search): books whose title or author contains the text, ignoring case. */
    public List<Book> search(String text) {
        if (text == null || text.isBlank()) {
            throw new com.library.exception.InvalidInputException("Search text cannot be empty.");
        }
        String needle = text.trim().toLowerCase(Locale.ROOT);
        return listBooks().stream()
                .filter(b -> b.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                          || b.getAuthor().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /**
     * UPDATE: changes title, author and/or year of an existing book. The ISBN identifies the
     * book and cannot be changed. A null or blank argument means "keep the current value".
     *
     * @throws BookNotFoundException if no book has this ISBN
     */
    public Book updateBook(String isbn, String newTitle, String newAuthor, String newYear) {
        Book current = getBook(isbn);   // also validates the ISBN and throws if missing

        String title  = isBlank(newTitle)  ? current.getTitle()  : BookValidator.validateTitle(newTitle);
        String author = isBlank(newAuthor) ? current.getAuthor() : BookValidator.validateAuthor(newAuthor);
        int year      = isBlank(newYear)   ? current.getPublicationYear() : BookValidator.validateYear(newYear);

        Book updated = new Book(current.getIsbn(), title, author, year);
        repository.update(updated);
        return updated;
    }

    /**
     * DELETE: removes the book and returns it (so the UI can say what was deleted).
     *
     * @throws BookNotFoundException if no book has this ISBN
     */
    public Book deleteBook(String isbn) {
        Book existing = getBook(isbn);
        repository.delete(existing.getIsbn());
        return existing;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
