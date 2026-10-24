# Before and After: Refactoring Walkthrough

Starting point: the Week 3 inventory system (all CRUD operations working, bugs fixed).
Goal: better structure, readability and efficiency **without changing behavior**.

## 1. Package structure (modularization)

| Before | After |
|--------|-------|
| One flat package `com.inventory` with `Item`, `InventoryService`, 2 exceptions, `Main` | `model`, `repository`, `service`, `util`, `exception`, `app` packages, each with one responsibility |

## 2. DRY: validation in one place

**Before** (`Item.java`): the same checks were written in the constructor and again in each setter.

```java
public void setQuantity(int quantity) {
    if (quantity < 0) {
        throw new IllegalArgumentException("Quantity must not be negative");
    }
    this.quantity = quantity;
}
public void setName(String name) {
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Item name must not be null or blank");
    }
    this.name = name.trim();
}
// ...same pattern for price and id
```

**After** (`Validators.java`, used by `Item`): each rule is written once.

```java
this.id       = Validators.requireNonBlank(id, "Item id");
this.name     = Validators.requireNonBlank(name, "Item name");
this.quantity = Validators.requireNonNegative(quantity, "Quantity");
this.price    = Validators.requireValidPrice(price);
```

## 3. DRY: one update helper

**Before** (`InventoryService`): every update method repeated the lookup.

```java
public void updateQuantity(String id, int q) { requireItem(id).setQuantity(q); }
public void updatePrice(String id, double p) { requireItem(id).setPrice(p); }
public void updateName(String id, String n)  { requireItem(id).setName(n); }
```

**After**: the "find, change, save" logic exists once and each method is a one-liner.

```java
public void updateQuantity(String id, int q) { update(id, item -> item.withQuantity(q)); }

private void update(String id, UnaryOperator<Item> change) {
    Item current = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
    repository.save(change.apply(current));   // invalid change throws BEFORE save
}
```

## 4. Design pattern: Repository

**Before**: the service owned the `HashMap` itself, so storage and business rules were tangled.

```java
public class InventoryService {
    private final Map<String, Item> items = new LinkedHashMap<>();
    ...
}
```

**After**: the service depends on the `ItemRepository` interface and receives it through the constructor
(Dependency Inversion). Storage can be swapped (file, database, test double) without touching the service.

```java
public InventoryService(ItemRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
}
```

## 5. Immutable `Item`

**Before**: public setters let any caller change an item behind the service's back.
**After**: `Item` is `final` with no setters; `withQuantity(...)`, `withPrice(...)`, `withName(...)`
return a new validated item. This is also what makes the cached total in section 7 safe.

## 6. Readability: streams instead of manual loops

**Before**
```java
List<Item> result = new ArrayList<>();
for (Item item : items.values()) {
    if (item.getQuantity() < threshold) {
        result.add(item);
    }
}
return result;
```
**After**
```java
return repository.stream()
        .filter(item -> item.quantity() < threshold)
        .toList();
```

## 7. Optimization A: total inventory value, O(n) to O(1)

**Before**: a full loop over every item on every call.
```java
double total = 0;
for (Item item : items.values()) { total += item.getTotalValue(); }
return total;
```
**After**: the repository keeps a running total, adjusted by the difference on each `save` / `delete`.
```java
Item previous = items.put(item.id(), item);
totalValue += item.totalValue() - (previous == null ? 0.0 : previous.totalValue());
```

## 8. Optimization B: search without repeated lower-casing

**Before**: every item name was lower-cased again on every search.
```java
if (item.getName().toLowerCase().contains(needle)) { ... }
```
**After**: `Item` computes `nameLower` once in its constructor (using `Locale.ROOT`, so results do not
depend on the machine's language settings).
```java
.filter(item -> item.nameLower().contains(needle))
```

## Measured performance

Machine: cloud sandbox, JDK 21. 100,000 items. Plain `System.nanoTime` benchmark with warm-up and
the median of 7 runs (`PerformanceComparison.java`). Three separate runs:

| Operation | Before (ms) | After (ms) | Speed-up |
|-----------|-------------|------------|----------|
| `getTotalInventoryValue()` x 2,000 | 1,833 to 2,255 | 0.04 | from ~2 seconds to effectively instant |
| `searchByName("ITEM 99")` x 200 | 588 to 668 | 300 to 403 | about 1.5x to 2.0x |

Numbers vary between runs and machines; use JMH for rigorous benchmarking. The search gain is smaller
than the total-value gain because it still scans every item once (O(n)); only the repeated string
work was removed.

## Trade-offs

- The running total is a `double`; repeated add/subtract can drift by tiny amounts. The repository
  resets it to exactly 0 when it becomes empty, and tests compare with a small delta. For real money,
  `BigDecimal` or integer cents would be the next step.
- `Item` accessors changed from `getX()` to `x()`, so callers had to be updated (tests were ported).
