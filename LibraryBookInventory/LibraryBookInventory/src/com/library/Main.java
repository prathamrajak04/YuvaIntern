package com.library;

import com.library.repository.BookRepository;
import com.library.repository.InMemoryBookRepository;
import com.library.service.BookService;
import com.library.ui.ConsoleInput;
import com.library.ui.ConsoleMenu;
import java.util.Scanner;

/**
 * Program entry point. Its only job is to create the objects and connect them:
 * repository -&gt; service -&gt; menu. No business logic belongs here.
 */
public class Main {
    public static void main(String[] args) {
        BookRepository repository = new InMemoryBookRepository();   // data storage
        BookService service = new BookService(repository);          // business rules
        // try-with-resources closes the Scanner when the program ends.
        try (Scanner scanner = new Scanner(System.in)) {
            new ConsoleMenu(service, new ConsoleInput(scanner)).run();  // user interface
        }
    }
}
