# Library Book Inventory (Java command-line application)

A menu-driven console program that manages a library's book inventory with full CRUD
(Create, Read, Update, Delete). All data is kept **in memory** using Java collections, as required.

## Features
1. **Add** a book (ISBN, title, author, publication year)
2. **List** all books (sorted by title, shown as a table)
3. **View** one book by ISBN
4. **Search** by part of a title or author (case-insensitive)
5. **Update** title, author and/or year (press Enter to keep a value; the ISBN cannot change)
6. **Delete** a book (asks for confirmation)
7. **Exit**

Input is validated and every error produces a friendly message - the program never crashes on bad input.

## Requirements
* JDK 17 or newer (check with `java -version` and `javac -version`)
* No other libraries, no build tool needed

## How to compile and run

From the project folder (the one that contains `src/` and this README):

**Linux / macOS**
```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.library.Main
```

**Windows (Command Prompt)**
```bat
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out com.library.Main
```

**IDE (IntelliJ IDEA / Eclipse / VS Code):** open the folder, mark `src` as the source root, run `com.library.Main`.

## How to run the tests
The tests need no extra library:
```bash
javac -d out $(find src test -name "*.java")
java -cp out com.library.BookServiceSelfTest
```
Expected last line: `14 passed, 0 failed`.

## Using the program (example)
```
Choose an option (1-7): 1
ISBN (10 or 13 digits): 978-0-13-468599-1
Title: Effective Java
Author: Joshua Bloch
Publication year: 2018
  Added: "Effective Java" by Joshua Bloch (2018), ISBN 9780134685991
```
A longer recorded session, including error cases, is in `docs/sample_session.txt`.

## Validation rules
| Field | Rule |
|---|---|
| ISBN | 10 or 13 digits; hyphens/spaces allowed and removed; ISBN-10 may end in `X`; must be unique |
| Title, Author | not blank, at most 200 characters (leading/trailing spaces removed) |
| Publication year | whole number from 1000 to the current year |
| Menu choice | number 1-7, anything else shows "Invalid option" |

## Project structure
```
src/com/library/
  Main.java                          entry point: connects repository, service and menu
  model/Book.java                    the Book entity (immutable)
  exception/                         LibraryException (base), BookNotFoundException,
                                     DuplicateBookException, InvalidInputException
  repository/BookRepository.java     storage contract (interface)
  repository/InMemoryBookRepository  LinkedHashMap<String, Book> keyed by ISBN
  service/BookValidator.java         all validation rules
  service/BookService.java           CRUD business logic
  ui/ConsoleInput.java               safe keyboard reading with re-prompting
  ui/ConsoleMenu.java                menu and dialogues
test/com/library/BookServiceSelfTest.java   14 automated checks
docs/                                design document (PDF) and sample session
```

## Design in one paragraph
`Main` creates an `InMemoryBookRepository`, gives it to `BookService`, and gives the service to `ConsoleMenu`.
The menu talks to the user, the service applies the rules and throws exceptions, and the repository stores
the books. Because each layer only knows the one below it (through an interface), the storage could later
become a file or a database without changing the service or the menu.
