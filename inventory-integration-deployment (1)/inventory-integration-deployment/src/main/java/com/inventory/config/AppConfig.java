package com.inventory.config;

import com.inventory.exception.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Immutable application settings, resolved once at startup.
 *
 * <p>Resolution order for the data file (later wins):
 * <ol>
 *   <li>bundled defaults ({@code /application.properties} inside the JAR)</li>
 *   <li>external config file passed with {@code --config=PATH}</li>
 *   <li>environment variable {@code INVENTORY_DATA_FILE}</li>
 *   <li>JVM system property {@code -Dinventory.data.file=PATH}</li>
 *   <li>command-line flag {@code --data=PATH}</li>
 * </ol>
 *
 * <p>DESIGN: environment and system properties are passed in as parameters (instead of being read
 * inside the method) so the precedence rules can be unit-tested without touching the real
 * process environment.
 */
public final class AppConfig {

    public static final String BUNDLED_RESOURCE = "/application.properties";
    public static final String ENV_DATA_FILE = "INVENTORY_DATA_FILE";
    public static final String SYS_PROP_DATA_FILE = "inventory.data.file";

    private final String appName;
    private final String version;
    private final Path dataFile;
    private final Path logFile;
    private final int lowStockThreshold;

    private AppConfig(String appName, String version, Path dataFile, Path logFile, int lowStockThreshold) {
        this.appName = appName;
        this.version = version;
        this.dataFile = dataFile;
        this.logFile = logFile;
        this.lowStockThreshold = lowStockThreshold;
    }

    /** Production entry point: reads the real environment and system properties. */
    public static AppConfig load(Path externalConfig, Path dataFileOverride) {
        return load(externalConfig, dataFileOverride, System.getenv(), System.getProperties());
    }

    /** Testable variant with explicit environment and system properties. */
    public static AppConfig load(Path externalConfig, Path dataFileOverride,
                                 Map<String, String> env, Properties systemProperties) {
        Properties props = new Properties();

        // 1. bundled defaults
        try (InputStream in = AppConfig.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new RepositoryException("Could not read bundled configuration", e);
        }

        // 2. external config file (must exist if the user asked for it)
        if (externalConfig != null) {
            if (!Files.isRegularFile(externalConfig)) {
                throw new IllegalArgumentException("Config file not found: " + externalConfig);
            }
            try (Reader reader = Files.newBufferedReader(externalConfig, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                throw new RepositoryException("Could not read config file " + externalConfig, e);
            }
        }

        // 3. environment variable and 4. system property
        applyIfPresent(props, "data.file", env.get(ENV_DATA_FILE));
        applyIfPresent(props, "data.file", systemProperties.getProperty(SYS_PROP_DATA_FILE));

        // 5. command-line flag
        if (dataFileOverride != null) {
            props.setProperty("data.file", dataFileOverride.toString());
        }

        return new AppConfig(
                props.getProperty("app.name", "Inventory Manager").trim(),
                props.getProperty("app.version", "unknown").trim(),
                Path.of(props.getProperty("data.file", "data/inventory.csv").trim()),
                Path.of(props.getProperty("log.file", "logs/inventory.log").trim()),
                parseThreshold(props.getProperty("low.stock.threshold", "5")));
    }

    private static void applyIfPresent(Properties props, String key, String value) {
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }

    private static int parseThreshold(String raw) {
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("low.stock.threshold must be a whole number but was '" + raw + "'");
        }
        if (value < 0) {
            throw new IllegalArgumentException("low.stock.threshold must not be negative but was " + value);
        }
        return value;
    }

    public String appName() { return appName; }
    public String version() { return version; }
    public Path dataFile() { return dataFile; }
    public Path logFile() { return logFile; }
    public int lowStockThreshold() { return lowStockThreshold; }

    @Override
    public String toString() {
        return "AppConfig{dataFile=" + dataFile + ", logFile=" + logFile
                + ", lowStockThreshold=" + lowStockThreshold + ", version=" + version + "}";
    }
}
