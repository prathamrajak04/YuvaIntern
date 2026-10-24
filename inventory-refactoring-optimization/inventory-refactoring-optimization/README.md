# Inventory Management: Refactoring and Optimization

[![CI](https://github.com/prathamrajak04/inventory-refactoring-optimization/actions/workflows/ci.yml/badge.svg)](https://github.com/prathamrajak04/inventory-refactoring-optimization/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![License](https://img.shields.io/badge/license-MIT-blue)

A Java inventory system (create, read, update, delete) that was refactored from a working but
tangled Week 3 version into a clean, modular and faster design, with the behavior locked in by a
50-test JUnit 5 suite.

## What changed

| Area | Before | After |
|------|--------|-------|
| Structure | One flat package | `model`, `repository`, `service`, `util`, `exception`, `app` |
| Duplication | Same validation copy-pasted in setters; 3 near-identical update methods | `Validators` utility + one `update()` helper |
| Design | Service owns the HashMap | Repository pattern with constructor injection |
| State | Mutable `Item` with setters | Immutable `Item` with `withXxx()` methods |
| Loops | Hand-written loops | Stream filters |
| Total value | O(n) loop on every call | O(1) running total |
| Search | Lower-cases every name on every call | Lower-case name cached once per item |

Full side-by-side code is in [`docs/BEFORE_AFTER.md`](docs/BEFORE_AFTER.md).

## Performance (100,000 items, JDK 21, median of 7 runs)

| Operation | Before | After |
|-----------|--------|-------|
| `getTotalInventoryValue()` x 2,000 | about 1.8 to 2.3 seconds | about 0.04 ms |
| `searchByName(...)` x 200 | about 590 to 670 ms | about 300 to 400 ms |

Reproduce with `src/test/java/com/inventory/benchmark/PerformanceComparison.java`
(run `main` from your IDE; it compares against the preserved Week 3 code in the same folder).

## Project structure

```
src/main/java/com/inventory/
  app/          Main demo
  exception/    ItemNotFoundException, DuplicateItemException
  model/        Item (immutable)
  repository/   ItemRepository (interface), InMemoryItemRepository
  service/      InventoryService (business logic)
  util/         Validators
src/test/java/com/inventory/
  model/ util/ repository/ service/   unit tests
  benchmark/                          Week 3 baseline + PerformanceComparison
docs/
  Week4_Refactoring_Report.docx       short report
  BEFORE_AFTER.md                     detailed before/after code
```

## Run it

Requirements: JDK 17+ and Maven 3.8+.

```bash
mvn test        # run the unit tests and generate the coverage report
```

Coverage report: `target/site/jacoco/index.html`. To see the demo, run `com.inventory.app.Main` from your IDE.

## License

MIT
