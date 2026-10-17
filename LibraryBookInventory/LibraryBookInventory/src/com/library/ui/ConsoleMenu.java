package com.library.ui;

import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.service.BookService;
import com.library.service.BookValidator;
import java.util.List;

/**
 * The menu-driven command-line interface. It only handles conversation with the user:
 * show menu, ask questions, print results. All rules live in {@link BookService}.
 *
 * <p>Error handling strategy: every menu action runs inside one try/catch in {@link #run()}.
 * Expected problems (invalid input, duplicate ISBN, book not found) arrive as LibraryException,
 * are shown as a short message, and the program returns to the menu - it never crashes.</p>
 */
public class ConsoleMenu {

    private static final String LINE = "-".repeat(78);

    private final BookService service;
    private final ConsoleInput in;

    public ConsoleMenu(BookService service, ConsoleInput in) {
        this.service = service;
        this.in = in;
    }

    /** Main loop: show menu, read choice, run the action, repeat until the user exits. */
    public void run() {
        System.out.println("=== Library Book Inventory ===");
        boolean running = true;
        try {
            while (running) {
                printMenu();
                int choice = in.readMenuChoice("Choose an option (1-7): ");
                try {
                    running = handle(choice);
                } catch (LibraryException e) {
                    // Expected, user-correctable problems end up here.
                    System.out.println("  ! " + e.getMessage());
                }
            }
        } catch (ConsoleInput.EndOfInputException e) {
            System.out.println("\nInput ended. Goodbye!");
        }
    }

    /** Runs the chosen action. Returns false only for "Exit". */
    private boolean handle(int choice) {
        switch (choice) {
            case 1 -> addBook();
            case 2 -> listBooks();
            case 3 -> viewBook();
            case 4 -> searchBooks();
            case 5 -> updateBook();
            case 6 -> deleteBook();
            case 7 -> {
                System.out.println("Goodbye!");
                return false;
            }
            default -> System.out.println("  ! Invalid option. Please enter a number from 1 to 7.");
        }
        return true;
    }

    private void printMenu() {
        System.out.println();
        System.out.println(LINE);
        System.out.println(" 1. Add a book");
        System.out.println(" 2. List all books");
        System.out.println(" 3. View a book by ISBN");
        System.out.println(" 4. Search by title or author");
        System.out.println(" 5. Update a book");
        System.out.println(" 6. Delete a book");
        System.out.println(" 7. Exit");
        System.out.println(LINE);
    }

    // ---------------------------------------------------------------- CREATE
    private void addBook() {
        System.out.println("\n-- Add a new book --");
        // Each question repeats until the answer passes validation, so one typo does not lose the others.
        String isbn   = in.readValid("ISBN (10 or 13 digits): ", BookValidator::validateIsbn);
        String title  = in.readValid("Title: ", BookValidator::validateTitle);
        String author = in.readValid("Author: ", BookValidator::validateAuthor);
        String year   = String.valueOf(in.readValid("Publication year: ", BookValidator::validateYear));

        // The service re-validates and rejects a duplicate ISBN (DuplicateBookException).
        Book added = service.addBook(isbn, title, author, year);
        System.out.println("  Added: " + added);
    }

    // ------------------------------------------------------------------ READ
    private void listBooks() {
        List<Book> books = service.listBooks();
        if (books.isEmpty()) {
            System.out.println("\nThe library has no books yet. Choose option 1 to add one.");
            return;
        }
        printTable(books);
    }

    private void viewBook() {
        String isbn = in.readValid("\nISBN of the book: ", BookValidator::validateIsbn);
        System.out.println("  " + service.getBook(isbn));
    }

    private void searchBooks() {
        String text = in.readLine("\nSearch text (part of a title or author): ");
        List<Book> found = service.search(text);
        if (found.isEmpty()) {
            System.out.println("  No books match \"" + text + "\".");
        } else {
            printTable(found);
        }
    }

    // ---------------------------------------------------------------- UPDATE
    private void updateBook() {
        String isbn = in.readValid("\nISBN of the book to update: ", BookValidator::validateIsbn);
        Book current = service.getBook(isbn);       // fails early if the book does not exist
        System.out.println("  Current: " + current);
        System.out.println("  (press Enter to keep the current value)");

        String title  = in.readOptional("New title [" + current.getTitle() + "]: ", BookValidator::validateTitle);
        String author = in.readOptional("New author [" + current.getAuthor() + "]: ", BookValidator::validateAuthor);
        String year   = in.readOptional("New year [" + current.getPublicationYear() + "]: ", BookValidator::validateYear);

        Book updated = service.updateBook(isbn, title, author, year);
        System.out.println("  Updated: " + updated);
    }

    // ---------------------------------------------------------------- DELETE
    private void deleteBook() {
        String isbn = in.readValid("\nISBN of the book to delete: ", BookValidator::validateIsbn);
        Book book = service.getBook(isbn);
        System.out.println("  " + book);
        // Deleting is irreversible (data is only in memory), so ask for confirmation.
        if (in.confirm("  Really delete this book?")) {
            service.deleteBook(isbn);
            System.out.println("  Deleted.");
        } else {
            System.out.println("  Cancelled. The book was not deleted.");
        }
    }

    // --------------------------------------------------------------- DISPLAY
    /** Prints books as an aligned table; long text is shortened with "..." to keep the columns tidy. */
    private void printTable(List<Book> books) {
        System.out.println();
        System.out.printf("%-14s %-32s %-22s %s%n", "ISBN", "Title", "Author", "Year");
        System.out.println(LINE);
        for (Book b : books) {
            System.out.printf("%-14s %-32s %-22s %d%n",
                    b.getIsbn(), shorten(b.getTitle(), 32), shorten(b.getAuthor(), 22), b.getPublicationYear());
        }
        System.out.println(LINE);
        System.out.println(books.size() + (books.size() == 1 ? " book" : " books"));
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
