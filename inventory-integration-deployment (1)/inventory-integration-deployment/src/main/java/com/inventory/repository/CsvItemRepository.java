package com.inventory.repository;

import com.inventory.exception.RepositoryException;
import com.inventory.model.Item;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File-backed repository: everything {@link InMemoryItemRepository} does, plus durable storage in
 * a CSV file so data survives restarts.
 *
 * <p>INTEGRATION DECISIONS
 * <ul>
 *   <li><b>Write first, then apply.</b> Every change builds the new full item list, writes it to
 *       disk, and only then updates memory. If the disk write fails, memory is untouched, so memory
 *       and disk can never disagree (no rollback logic needed, insertion order preserved).</li>
 *   <li><b>Atomic replace.</b> The file is written to {@code name.tmp} and moved over the real file,
 *       so a crash mid-write cannot leave a half-written data file.</li>
 *   <li><b>Loading goes through {@code super.save}.</b> That keeps the O(1) running total correct
 *       and avoids re-writing the file once per loaded row.</li>
 *   <li><b>Fail loudly on corrupt data.</b> A bad line stops startup with the file name and line
 *       number instead of silently dropping the item.</li>
 * </ul>
 *
 * <p>KNOWN LIMITS (see developer guide): each change rewrites the whole file (O(n)), and the class is
 * not thread-safe or safe for two processes sharing one file.
 */
public class CsvItemRepository extends InMemoryItemRepository {

    private static final Logger LOG = Logger.getLogger(CsvItemRepository.class.getName());

    private final Path file;

    /**
     * Loads existing data immediately. The compiler's "this-escape" lint is suppressed on purpose:
     * load() only calls InMemoryItemRepository's own methods via {@code super.*} and never calls
     * {@link #write} or any other method a subclass is meant to override, so a subclass cannot
     * observe a half-built object.
     */
    @SuppressWarnings("this-escape")
    public CsvItemRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        load();
    }

    // ------------------------------------------------------------------ loading

    private void load() {
        if (!Files.exists(file)) {
            LOG.log(Level.INFO, "No data file at {0}; starting with an empty inventory", file);
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositoryException("Could not read data file " + file, e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            if (line.isBlank() || (i == 0 && line.trim().equals(CsvCodec.HEADER))) {
                continue;
            }
            Item item;
            try {
                item = CsvCodec.decode(line);
            } catch (IllegalArgumentException e) {
                throw new RepositoryException(
                        "Corrupt data file " + file + " at line " + lineNumber + ": " + e.getMessage(), e);
            }
            if (super.existsById(item.id())) { // super.*: constructor must not call overridable methods
                throw new RepositoryException(
                        "Corrupt data file " + file + " at line " + lineNumber + ": duplicate id '" + item.id() + "'");
            }
            super.save(item); // explicit parent call: loading must not trigger a disk write
        }
        LOG.log(Level.INFO, "Loaded {0} item(s) from {1}", new Object[] {super.count(), file});
    }

    // ------------------------------------------------------------------ writes

    @Override
    public void save(Item item) {
        Objects.requireNonNull(item, "item");
        List<Item> next = new ArrayList<>(findAll());
        boolean replaced = false;
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).id().equals(item.id())) {
                next.set(i, item);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            next.add(item);
        }
        write(next);     // 1. durable first
        super.save(item); // 2. then memory
    }

    @Override
    public boolean deleteById(String id) {
        if (!existsById(id)) {
            return false;
        }
        List<Item> next = new ArrayList<>(findAll());
        next.removeIf(item -> item.id().equals(id));
        write(next);
        return super.deleteById(id);
    }

    /**
     * Writes the complete item list to disk atomically. Protected so tests can simulate a failing
     * disk by overriding it.
     */
    protected void write(List<Item> items) {
        try {
            Path absolute = file.toAbsolutePath();
            Path parent = absolute.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");

            List<String> lines = new ArrayList<>(items.size() + 1);
            lines.add(CsvCodec.HEADER);
            for (Item item : items) {
                lines.add(CsvCodec.encode(item));
            }
            Files.write(temp, lines, StandardCharsets.UTF_8);

            try {
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); // fallback for exotic file systems
            }
        } catch (IOException e) {
            throw new RepositoryException("Could not write data file " + file, e);
        }
    }

    public Path file() {
        return file;
    }
}
