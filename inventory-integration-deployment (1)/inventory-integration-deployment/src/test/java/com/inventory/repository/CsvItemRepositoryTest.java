package com.inventory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventory.exception.RepositoryException;
import com.inventory.model.Item;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CsvItemRepository (persistence)")
class CsvItemRepositoryTest {

    private Path newDataFile() throws IOException {
        return Files.createTempDirectory("inventory-csv-test").resolve("inventory.csv");
    }

    /** A repository whose disk always fails - simulates a full or read-only disk. */
    private static CsvItemRepository failingDisk(Path file) {
        return new CsvItemRepository(file) {
            @Override
            protected void write(List<Item> items) {
                throw new RepositoryException("disk full");
            }
        };
    }

    @Test
    @DisplayName("missing file starts empty and does not create a file yet")
    void missingFileStartsEmpty() throws Exception {
        Path file = newDataFile();
        CsvItemRepository repository = new CsvItemRepository(file);
        assertEquals(0, repository.count());
        assertFalse(Files.exists(file));
    }

    @Test
    @DisplayName("save writes header + row, and a new instance reads them back")
    void survivesRestart() throws Exception {
        Path file = newDataFile();
        CsvItemRepository first = new CsvItemRepository(file);
        first.save(new Item("A1", "Laptop", 10, 50000));
        first.save(new Item("A2", "Mouse", 3, 500));

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals("id,name,quantity,price", lines.get(0));
        assertEquals(3, lines.size());

        CsvItemRepository second = new CsvItemRepository(file);
        assertEquals(2, second.count());
        assertEquals("Laptop", second.findById("A1").orElseThrow().name());
        assertEquals(501500.0, second.totalValue(), 0.0001);
    }

    @Test
    @DisplayName("updating an item rewrites its row instead of adding a new one")
    void updatePersists() throws Exception {
        Path file = newDataFile();
        CsvItemRepository repository = new CsvItemRepository(file);
        repository.save(new Item("A1", "Laptop", 10, 50000));
        repository.save(new Item("A1", "Laptop", 7, 50000));

        CsvItemRepository reloaded = new CsvItemRepository(file);
        assertEquals(1, reloaded.count());
        assertEquals(7, reloaded.findById("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("delete is persisted")
    void deletePersists() throws Exception {
        Path file = newDataFile();
        CsvItemRepository repository = new CsvItemRepository(file);
        repository.save(new Item("A1", "Laptop", 10, 50000));
        repository.save(new Item("A2", "Mouse", 3, 500));
        assertTrue(repository.deleteById("A1"));
        assertFalse(repository.deleteById("A1"));

        CsvItemRepository reloaded = new CsvItemRepository(file);
        assertEquals(1, reloaded.count());
        assertTrue(reloaded.findById("A1").isEmpty());
    }

    @Test
    @DisplayName("insertion order is preserved across restarts, even after an update")
    void orderPreserved() throws Exception {
        Path file = newDataFile();
        CsvItemRepository repository = new CsvItemRepository(file);
        repository.save(new Item("C", "Third", 1, 1));
        repository.save(new Item("A", "First", 1, 1));
        repository.save(new Item("B", "Second", 1, 1));
        repository.save(new Item("C", "Third", 5, 1)); // update must not move it

        List<String> ids = new CsvItemRepository(file).findAll().stream().map(Item::id).toList();
        assertEquals(List.of("C", "A", "B"), ids);
    }

    @Test
    @DisplayName("names with commas and quotes survive a restart")
    void specialCharactersSurviveRestart() throws Exception {
        Path file = newDataFile();
        new CsvItemRepository(file).save(new Item("A1", "Cable, \"USB-C\"", 2, 99.5));
        assertEquals("Cable, \"USB-C\"", new CsvItemRepository(file).findById("A1").orElseThrow().name());
    }

    @Test
    @DisplayName("non-ASCII names (UTF-8) survive a restart")
    void unicodeSurvivesRestart() throws Exception {
        Path file = newDataFile();
        new CsvItemRepository(file).save(new Item("H1", "हल्दी Turmeric – 500g", 2, 80));
        assertEquals("हल्दी Turmeric – 500g",
                new CsvItemRepository(file).findById("H1").orElseThrow().name());
    }

    @Test
    @DisplayName("missing parent folders are created on first write")
    void createsParentFolders() throws Exception {
        Path file = Files.createTempDirectory("inventory-csv-test").resolve("a/b/c/inventory.csv");
        new CsvItemRepository(file).save(new Item("A1", "Laptop", 1, 1));
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("no temp file is left behind after a successful write")
    void noTempFileLeftBehind() throws Exception {
        Path file = newDataFile();
        new CsvItemRepository(file).save(new Item("A1", "Laptop", 1, 1));
        assertFalse(Files.exists(file.resolveSibling("inventory.csv.tmp")));
    }

    @Test
    @DisplayName("file without header and with blank lines still loads")
    void headerOptionalAndBlankLinesSkipped() throws Exception {
        Path file = newDataFile();
        Files.writeString(file, "A1,Laptop,10,50000\n\nA2,Mouse,3,500\n");
        assertEquals(2, new CsvItemRepository(file).count());
    }

    @Test
    @DisplayName("Windows line endings (CRLF) load correctly")
    void crlfLoads() throws Exception {
        Path file = newDataFile();
        Files.writeString(file, "id,name,quantity,price\r\nA1,Laptop,10,50000\r\n");
        assertEquals(1, new CsvItemRepository(file).count());
    }

    @Test
    @DisplayName("corrupt line stops startup and names the file and the line number")
    void corruptLineReportsLineNumber() throws Exception {
        Path file = newDataFile();
        Files.writeString(file, "id,name,quantity,price\nA1,Laptop,10,50000\nA2,Mouse,many,500\n");
        RepositoryException ex = assertThrows(RepositoryException.class, () -> new CsvItemRepository(file));
        assertTrue(ex.getMessage().contains("line 3"));
        assertTrue(ex.getMessage().contains("inventory.csv"));
    }

    @Test
    @DisplayName("duplicate id in the file stops startup")
    void duplicateIdInFile() throws Exception {
        Path file = newDataFile();
        Files.writeString(file, "id,name,quantity,price\nA1,Laptop,10,50000\nA1,Other,1,1\n");
        RepositoryException ex = assertThrows(RepositoryException.class, () -> new CsvItemRepository(file));
        assertTrue(ex.getMessage().contains("duplicate id 'A1'"));
        assertTrue(ex.getMessage().contains("line 3"));
    }

    @Test
    @DisplayName("data path that is a directory gives a RepositoryException, not a raw IOException")
    void directoryAsDataFile() throws Exception {
        Path directory = Files.createTempDirectory("inventory-csv-test");
        assertThrows(RepositoryException.class, () -> new CsvItemRepository(directory));
    }

    // ---------------------------------------------------------------- failure consistency

    @Test
    @DisplayName("failed write on ADD leaves memory unchanged")
    void failedAddLeavesMemoryUnchanged() throws Exception {
        CsvItemRepository repository = failingDisk(newDataFile());
        assertThrows(RepositoryException.class, () -> repository.save(new Item("A1", "Laptop", 1, 1)));
        assertEquals(0, repository.count());
        assertEquals(0.0, repository.totalValue(), 0.0);
    }

    @Test
    @DisplayName("failed write on UPDATE keeps the old item and the old total")
    void failedUpdateKeepsOldItem() throws Exception {
        Path file = newDataFile();
        new CsvItemRepository(file).save(new Item("A1", "Laptop", 10, 100));

        CsvItemRepository repository = failingDisk(file);
        assertThrows(RepositoryException.class, () -> repository.save(new Item("A1", "Laptop", 99, 100)));
        assertEquals(10, repository.findById("A1").orElseThrow().quantity());
        assertEquals(1000.0, repository.totalValue(), 0.0001);
    }

    @Test
    @DisplayName("failed write on DELETE keeps the item")
    void failedDeleteKeepsItem() throws Exception {
        Path file = newDataFile();
        new CsvItemRepository(file).save(new Item("A1", "Laptop", 10, 100));

        CsvItemRepository repository = failingDisk(file);
        assertThrows(RepositoryException.class, () -> repository.deleteById("A1"));
        assertTrue(repository.existsById("A1"));
        assertEquals(1000.0, repository.totalValue(), 0.0001);
    }
}
