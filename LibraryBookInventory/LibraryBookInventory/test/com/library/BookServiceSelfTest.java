package com.library;

import com.library.exception.*;
import com.library.model.Book;
import com.library.repository.InMemoryBookRepository;
import com.library.service.BookService;
import java.util.List;

/**
 * Self-checking tests with no external library, so they run with plain "java".
 * Each check() prints PASS or FAIL; the program exits with code 1 if anything failed.
 */
public class BookServiceSelfTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        // ---- CREATE
        test("add stores a book and normalises the ISBN", () -> {
            BookService s = fresh();
            Book b = s.addBook("978-0-13-468599-1", "  Effective Java ", "Joshua Bloch", "2018");
            expect(b.getIsbn().equals("9780134685991"), "hyphens removed");
            expect(b.getTitle().equals("Effective Java"), "title trimmed");
        });
        test("duplicate ISBN is rejected", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "A", "B", "2018");
            expectThrows(DuplicateBookException.class, () -> s.addBook("978-0-13-468599-1", "C", "D", "2000"));
        });
        test("blank title / author rejected", () -> {
            BookService s = fresh();
            expectThrows(InvalidInputException.class, () -> s.addBook("9780134685991", "  ", "B", "2018"));
            expectThrows(InvalidInputException.class, () -> s.addBook("9780134685991", "A", "", "2018"));
        });
        test("bad ISBN rejected (letters, wrong length, empty)", () -> {
            BookService s = fresh();
            expectThrows(InvalidInputException.class, () -> s.addBook("12345", "A", "B", "2018"));
            expectThrows(InvalidInputException.class, () -> s.addBook("97801346859AB", "A", "B", "2018"));
            expectThrows(InvalidInputException.class, () -> s.addBook("", "A", "B", "2018"));
        });
        test("ISBN-10 ending in X accepted", () -> {
            expect(fresh().addBook("0-8044-2957-X", "A", "B", "1990").getIsbn().equals("080442957X"), "x kept");
        });
        test("year must be a number in range", () -> {
            BookService s = fresh();
            expectThrows(InvalidInputException.class, () -> s.addBook("9780134685991", "A", "B", "abc"));
            expectThrows(InvalidInputException.class, () -> s.addBook("9780134685991", "A", "B", "999"));
            expectThrows(InvalidInputException.class, () -> s.addBook("9780134685991", "A", "B", "3000"));
            expectThrows(InvalidInputException.class, () -> s.addBook("9780134685991", "A", "B", ""));
        });
        // ---- READ
        test("getBook finds by ISBN (with hyphens) and throws when missing", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "Effective Java", "Joshua Bloch", "2018");
            expect(s.getBook("978-0-13-468599-1").getAuthor().equals("Joshua Bloch"), "found");
            expectThrows(BookNotFoundException.class, () -> s.getBook("9780132350884"));
        });
        test("listBooks is sorted by title ignoring case", () -> {
            BookService s = fresh();
            s.addBook("9780000000002", "zebra", "A", "2001");
            s.addBook("9780000000019", "Apple", "A", "2001");
            s.addBook("9780000000026", "mango", "A", "2001");
            List<Book> l = s.listBooks();
            expect(l.get(0).getTitle().equals("Apple") && l.get(1).getTitle().equals("mango") && l.get(2).getTitle().equals("zebra"), "order");
        });
        test("search matches title or author, case-insensitive", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "Effective Java", "Joshua Bloch", "2018");
            s.addBook("9780132350884", "Clean Code", "Robert Martin", "2008");
            expect(s.search("JAVA").size() == 1, "title");
            expect(s.search("martin").size() == 1, "author");
            expect(s.search("python").isEmpty(), "none");
            expectThrows(InvalidInputException.class, () -> s.search("  "));
        });
        // ---- UPDATE
        test("update changes only the supplied fields", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "Old", "Author", "2000");
            Book u = s.updateBook("9780134685991", "New", "", "");
            expect(u.getTitle().equals("New") && u.getAuthor().equals("Author") && u.getPublicationYear() == 2000, "partial update");
            expect(s.getBook("9780134685991").getTitle().equals("New"), "persisted");
        });
        test("update validates new values and requires an existing book", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "Old", "Author", "2000");
            expectThrows(InvalidInputException.class, () -> s.updateBook("9780134685991", "", "", "abc"));
            expect(s.getBook("9780134685991").getPublicationYear() == 2000, "unchanged after failed update");
            expectThrows(BookNotFoundException.class, () -> s.updateBook("9780132350884", "X", "", ""));
        });
        // ---- DELETE
        test("delete removes the book; deleting again fails", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "A", "B", "2000");
            s.deleteBook("9780134685991");
            expect(s.listBooks().isEmpty(), "gone");
            expectThrows(BookNotFoundException.class, () -> s.deleteBook("9780134685991"));
        });
        test("same ISBN can be added again after deletion", () -> {
            BookService s = fresh();
            s.addBook("9780134685991", "A", "B", "2000");
            s.deleteBook("9780134685991");
            s.addBook("9780134685991", "A2", "B", "2001");
            expect(s.listBooks().size() == 1, "re-added");
        });
        test("returned list is a copy (cannot corrupt the repository)", () -> {
            InMemoryBookRepository repo = new InMemoryBookRepository();
            repo.add(new Book("9780134685991", "A", "B", 2000));
            repo.findAll().clear();
            expect(repo.findAll().size() == 1, "still there");
        });

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static BookService fresh() { return new BookService(new InMemoryBookRepository()); }

    private static void test(String name, Runnable body) {
        try { body.run(); passed++; System.out.println("PASS  " + name); }
        catch (Throwable t) { failed++; System.out.println("FAIL  " + name + "  -> " + t); }
    }
    private static void expect(boolean condition, String what) {
        if (!condition) throw new AssertionError("expected: " + what);
    }
    private static void expectThrows(Class<? extends Throwable> type, Runnable body) {
        try { body.run(); } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
