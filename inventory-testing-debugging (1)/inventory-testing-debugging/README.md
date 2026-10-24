# Inventory Management: Unit Testing and Debugging

[![CI](https://github.com/YOUR-USERNAME/inventory-testing-debugging/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR-USERNAME/inventory-testing-debugging/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![License](https://img.shields.io/badge/license-MIT-blue)

A small Java inventory system (create, read, update, delete) with a JUnit 5 test suite and a documented
debugging walkthrough. It shows how tests expose real bugs and how each one is traced and fixed.

## Features

- Full CRUD on inventory items (`InventoryService`)
- Search by name (case-insensitive, partial match), low-stock report, total stock value
- Input validation in `Item` (blank id or name, negative quantity, NaN/infinite price)
- Custom exceptions: `DuplicateItemException`, `ItemNotFoundException`
- JUnit 5 tests with `@Nested`, `@BeforeEach` and `@ParameterizedTest`
- JaCoCo coverage report and GitHub Actions CI

## Project structure

```
src/main/java/com/inventory/   Item, InventoryService, exceptions, Main demo
src/test/java/com/inventory/   ItemTest, InventoryServiceTest
docs/Week3_Debugging_Report.docx   bug report and debugging approach
.github/workflows/ci.yml           runs mvn test on every push
```

## Run it

Requirements: JDK 17+ and Maven 3.8+.

```bash
mvn test        # run all unit tests + coverage report
```

To see the demo, run `com.inventory.Main` from your IDE.

The coverage report is generated at `target/site/jacoco/index.html`.

## Bugs found and fixed

| # | Bug | Root cause | Fix |
|---|-----|-----------|-----|
| 1 | Duplicate id overwrote an existing item | `put()` with no existence check | Throw `DuplicateItemException` |
| 2 | Search missed "Laptop" when typing "laptop" | Case-sensitive `contains` | Compare in lower case |
| 3 | Low-stock list included items exactly at the threshold | `<=` instead of `<` | Strict comparison |
| 4 | Negative quantity could be stored | Update bypassed validation via an unchecked setter | Use validating setter, remove unchecked one |
| 5 | Deleting an unknown id silently succeeded | `remove()` result ignored | Throw `ItemNotFoundException` |

## Debugging approach

1. Write a test (or small probe) that captures the expected behaviour.
2. Watch it fail and read the failure message.
3. Trace the failing code path to the root cause.
4. Apply the smallest fix, then rerun the whole suite to catch regressions.

## License

MIT
