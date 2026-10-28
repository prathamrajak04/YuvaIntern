package com.inventory.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppConfig (settings and precedence)")
class AppConfigTest {

    private static final Map<String, String> NO_ENV = Map.of();

    private Path writeConfig(String content) throws IOException {
        Path file = Files.createTempDirectory("inventory-config-test").resolve("app.properties");
        Files.writeString(file, content);
        return file;
    }

    @Test
    @DisplayName("bundled defaults are used when nothing else is given")
    void bundledDefaults() {
        AppConfig config = AppConfig.load(null, null, NO_ENV, new Properties());
        assertEquals(Path.of("data/inventory.csv"), config.dataFile());
        assertEquals(Path.of("logs/inventory.log"), config.logFile());
        assertEquals(5, config.lowStockThreshold());
        assertEquals("Inventory Manager", config.appName());
        assertFalse(config.version().isBlank());
    }

    @Test
    @DisplayName("external config file overrides the defaults")
    void externalFileOverrides() throws Exception {
        Path external = writeConfig("data.file=/srv/inv/stock.csv\nlow.stock.threshold=12\n");
        AppConfig config = AppConfig.load(external, null, NO_ENV, new Properties());
        assertEquals(Path.of("/srv/inv/stock.csv"), config.dataFile());
        assertEquals(12, config.lowStockThreshold());
        assertEquals(Path.of("logs/inventory.log"), config.logFile()); // untouched keys keep defaults
    }

    @Test
    @DisplayName("precedence: file < env < system property < command line")
    void precedenceChain() throws Exception {
        Path external = writeConfig("data.file=from-file.csv\n");
        Map<String, String> env = Map.of(AppConfig.ENV_DATA_FILE, "from-env.csv");
        Properties sys = new Properties();
        sys.setProperty(AppConfig.SYS_PROP_DATA_FILE, "from-sysprop.csv");

        assertEquals(Path.of("from-file.csv"),
                AppConfig.load(external, null, NO_ENV, new Properties()).dataFile());
        assertEquals(Path.of("from-env.csv"),
                AppConfig.load(external, null, env, new Properties()).dataFile());
        assertEquals(Path.of("from-sysprop.csv"),
                AppConfig.load(external, null, env, sys).dataFile());
        assertEquals(Path.of("from-cli.csv"),
                AppConfig.load(external, Path.of("from-cli.csv"), env, sys).dataFile());
    }

    @Test
    @DisplayName("blank environment variable is ignored")
    void blankEnvIgnored() {
        AppConfig config = AppConfig.load(null, null, Map.of(AppConfig.ENV_DATA_FILE, "   "), new Properties());
        assertEquals(Path.of("data/inventory.csv"), config.dataFile());
    }

    @Test
    @DisplayName("missing external config file is an error, not silently ignored")
    void missingConfigFile() {
        Path missing = Path.of("definitely", "not", "here.properties");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(missing, null, NO_ENV, new Properties()));
        assertTrue(ex.getMessage().contains("Config file not found"));
    }

    @Test
    @DisplayName("non-numeric threshold is rejected with the offending value")
    void badThreshold() throws Exception {
        Path external = writeConfig("low.stock.threshold=lots\n");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.load(external, null, NO_ENV, new Properties()));
        assertTrue(ex.getMessage().contains("lots"));
    }

    @Test
    @DisplayName("negative threshold is rejected; zero is allowed")
    void thresholdBoundary() throws Exception {
        Path negative = writeConfig("low.stock.threshold=-1\n");
        assertThrows(IllegalArgumentException.class, () -> AppConfig.load(negative, null, NO_ENV, new Properties()));
        Path zero = writeConfig("low.stock.threshold=0\n");
        assertEquals(0, AppConfig.load(zero, null, NO_ENV, new Properties()).lowStockThreshold());
    }

    @Test
    @DisplayName("whitespace around values is trimmed")
    void valuesTrimmed() throws Exception {
        Path external = writeConfig("data.file =   spaced.csv   \nlow.stock.threshold= 8 \n");
        AppConfig config = AppConfig.load(external, null, NO_ENV, new Properties());
        assertEquals(Path.of("spaced.csv"), config.dataFile());
        assertEquals(8, config.lowStockThreshold());
    }
}
