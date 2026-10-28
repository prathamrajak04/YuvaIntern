package com.inventory.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests: real Main, real config, real CSV file, real menu, and "restarts" by calling
 * Main.run again. These are the tests that prove the modules work TOGETHER.
 */
@DisplayName("Main (full application integration)")
class MainIntegrationTest {

    private static final class Result {
        int code;
        String out;
        String err;
    }

    private Path dir;

    /** Creates an isolated sandbox folder and a config file pointing every path inside it. */
    private Path sandbox() throws IOException {
        dir = Files.createTempDirectory("inventory-e2e");
        Properties props = new Properties();
        props.setProperty("data.file", dir.resolve("data").resolve("inventory.csv").toString());
        props.setProperty("log.file", dir.resolve("logs").resolve("inventory.log").toString());
        props.setProperty("low.stock.threshold", "5");
        Path config = dir.resolve("app.properties");
        try (OutputStream out = Files.newOutputStream(config)) {
            props.store(out, "test config"); // store() escapes Windows backslashes correctly
        }
        return config;
    }

    private Result run(String stdin, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Result result = new Result();
        result.code = Main.run(args,
                new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        result.out = out.toString(StandardCharsets.UTF_8);
        result.err = err.toString(StandardCharsets.UTF_8);
        return result;
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    @Test
    @DisplayName("--help prints usage and exits 0")
    void help() {
        Result result = run("", "--help");
        assertEquals(0, result.code);
        assertTrue(result.out.contains("Usage:"));
        assertTrue(result.out.contains("--config=PATH"));
    }

    @Test
    @DisplayName("--version prints name and version and exits 0")
    void version() {
        Result result = run("", "--version");
        assertEquals(0, result.code);
        assertTrue(result.out.startsWith("Inventory Manager "));
    }

    @Test
    @DisplayName("unknown argument exits 2 with a message on stderr")
    void unknownArgument() {
        Result result = run("", "--bogus");
        assertEquals(2, result.code);
        assertTrue(result.err.contains("Unknown argument: --bogus"));
        assertEquals("", result.out);
    }

    @Test
    @DisplayName("missing config file exits 1 with Startup failed")
    void missingConfig() {
        Result result = run("", "--config=/no/such/file.properties");
        assertEquals(1, result.code);
        assertTrue(result.err.contains("Startup failed: Config file not found"));
    }

    @Test
    @DisplayName("data survives restarts: add, restart, list, delete, restart")
    void dataSurvivesRestarts() throws Exception {
        Path config = sandbox();
        String configArg = "--config=" + config;

        // --- session 1: fresh install, add two items
        Result first = run(lines("1", "A1", "Laptop", "10", "50000", "1", "A2", "Mouse", "3", "500", "0"), configArg);
        assertEquals(0, first.code);
        assertTrue(first.out.contains("Loaded 0 item(s)."));
        assertTrue(Files.exists(dir.resolve("data").resolve("inventory.csv")));

        // --- session 2: "restart" - items must be loaded from disk
        Result second = run(lines("2", "10", "0"), configArg);
        assertEquals(0, second.code);
        assertTrue(second.out.contains("Loaded 2 item(s)."));
        assertTrue(second.out.contains("Laptop"));
        assertTrue(second.out.contains("Total inventory value: 501500.00"));

        // --- session 3: delete one
        Result third = run(lines("8", "A1", "0"), configArg);
        assertEquals(0, third.code);
        assertTrue(third.out.contains("Deleted item A1."));

        // --- session 4: only Mouse is left
        Result fourth = run(lines("2", "0"), configArg);
        assertTrue(fourth.out.contains("Loaded 1 item(s)."));
        assertFalse(fourth.out.contains("Laptop"));
        assertTrue(fourth.out.contains("Mouse"));
    }

    @Test
    @DisplayName("business-rule errors in the menu do not damage the stored data")
    void failedCommandsDoNotCorruptData() throws Exception {
        Path config = sandbox();
        String configArg = "--config=" + config;
        run(lines("1", "A1", "Laptop", "10", "50000", "0"), configArg);

        // duplicate add, negative quantity, unknown id: all should be rejected
        Result bad = run(lines("1", "A1", "Fake", "1", "1", "5", "A1", "-9", "8", "ZZZ", "0"), configArg);
        assertEquals(0, bad.code);
        assertTrue(bad.out.contains("Error: Item already exists: A1"));
        assertTrue(bad.out.contains("Error: Quantity must not be negative"));
        assertTrue(bad.out.contains("Error: Item not found: ZZZ"));

        Result check = run(lines("2", "0"), configArg);
        assertTrue(check.out.contains("Loaded 1 item(s)."));
        assertTrue(check.out.contains("Laptop"));
        assertFalse(check.out.contains("Fake"));
    }

    @Test
    @DisplayName("a log file is written")
    void logFileWritten() throws Exception {
        Path config = sandbox();
        run(lines("1", "A1", "Laptop", "10", "50000", "0"), "--config=" + config);
        String log = Files.readString(dir.resolve("logs").resolve("inventory.log"));
        assertTrue(log.contains("Added item A1"));
    }

    @Test
    @DisplayName("corrupt data file exits 1 and names the line")
    void corruptDataFile() throws Exception {
        Path config = sandbox();
        Files.createDirectories(dir.resolve("data"));
        Files.writeString(dir.resolve("data").resolve("inventory.csv"),
                "id,name,quantity,price\nA1,Laptop,oops,5\n");
        Result result = run("", "--config=" + config);
        assertEquals(1, result.code);
        assertTrue(result.err.contains("Startup failed: Corrupt data file"));
        assertTrue(result.err.contains("line 2"));
    }

    @Test
    @DisplayName("--data overrides the data file from the config")
    void dataFlagOverridesConfig() throws Exception {
        Path config = sandbox();
        Path other = dir.resolve("other.csv");
        Result result = run(lines("1", "X1", "Widget", "1", "2", "0"), "--config=" + config, "--data=" + other);
        assertEquals(0, result.code);
        assertTrue(Files.exists(other));
        assertFalse(Files.exists(dir.resolve("data").resolve("inventory.csv")));
    }
}
