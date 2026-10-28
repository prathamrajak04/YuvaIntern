package com.inventory.app;

import com.inventory.cli.ConsoleMenu;
import com.inventory.config.AppConfig;
import com.inventory.exception.RepositoryException;
import com.inventory.repository.CsvItemRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.service.InventoryService;
import com.inventory.util.LoggingSetup;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Entry point and COMPOSITION ROOT: the only place that knows which concrete classes are used and
 * how they are connected.
 *
 * <pre>
 *   AppConfig  ->  CsvItemRepository  ->  InventoryService  ->  ConsoleMenu
 *   (settings)     (storage)              (business rules)      (user interface)
 * </pre>
 *
 * Swapping the CSV file for a database later means changing exactly one line below.
 *
 * <p>Exit codes: 0 = normal, 1 = startup failure (bad config / corrupt data), 2 = bad command line.
 */
public final class Main {

    static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar inventory-app.jar [options]",
            "  --config=PATH   external properties file (overrides bundled defaults)",
            "  --data=PATH     CSV data file (highest priority)",
            "  --version, -v   print the version and exit",
            "  --help, -h      print this help and exit");

    private Main() {
        // not instantiable
    }

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, System.err));
    }

    /** Testable entry point: all I/O is passed in and the exit code is returned, not executed. */
    public static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        try {
            Path configPath = null;
            Path dataPath = null;
            boolean help = false;
            boolean version = false;

            for (String arg : args) {
                if (arg.equals("--help") || arg.equals("-h")) {
                    help = true;
                } else if (arg.equals("--version") || arg.equals("-v")) {
                    version = true;
                } else if (arg.startsWith("--config=")) {
                    configPath = Path.of(arg.substring("--config=".length()));
                } else if (arg.startsWith("--data=")) {
                    dataPath = Path.of(arg.substring("--data=".length()));
                } else {
                    err.println("Unknown argument: " + arg);
                    err.println(USAGE);
                    return 2;
                }
            }
            if (help) {
                out.println(USAGE);
                return 0;
            }

            AppConfig config = AppConfig.load(configPath, dataPath);
            if (version) {
                out.println(config.appName() + " " + config.version());
                return 0;
            }

            LoggingSetup.init(config.logFile());
            ItemRepository repository = new CsvItemRepository(config.dataFile());
            InventoryService service = new InventoryService(repository);

            out.println("Data file: " + config.dataFile().toAbsolutePath());
            out.println("Loaded " + service.size() + " item(s).");
            return new ConsoleMenu(service, in, out, config.lowStockThreshold()).run();

        } catch (IllegalArgumentException | RepositoryException e) {
            err.println("Startup failed: " + e.getMessage());
            return 1;
        }
    }
}
