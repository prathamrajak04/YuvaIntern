package com.inventory.benchmark;

import com.inventory.model.Item;
import com.inventory.service.InventoryService;
import java.util.Arrays;
import java.util.function.IntSupplier;

/**
 * Simple before/after timing of the two optimized operations.
 * Run it from your IDE (it is not a JUnit test, so "mvn test" does not execute it).
 *
 * This is a plain System.nanoTime micro-benchmark with warm-up and the median of several runs.
 * It is good enough to show the order of magnitude; use JMH for publication-grade numbers.
 */
public class PerformanceComparison {

    private static final int ITEMS = 100_000;
    private static final int TOTAL_CALLS = 2_000;
    private static final int SEARCH_CALLS = 200;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 7;

    /** Written to so the JIT cannot remove the benchmarked calls as dead code. */
    private static long sink;

    public static void main(String[] args) {
        LegacyInventoryService legacy = new LegacyInventoryService();
        InventoryService refactored = new InventoryService();

        for (int i = 0; i < ITEMS; i++) {
            String id = "SKU-" + i;
            String name = "Product Item " + i;
            int qty = i % 50;
            double price = 10 + (i % 100);
            legacy.addItem(new LegacyItem(id, name, qty, price));
            refactored.addItem(new Item(id, name, qty, price));
        }

        // Correctness first: both versions must return identical answers.
        double legacyTotal = legacy.getTotalInventoryValue();
        double newTotal = refactored.getTotalInventoryValue();
        if (Math.abs(legacyTotal - newTotal) > 1e-3) {
            throw new IllegalStateException("Totals differ: " + legacyTotal + " vs " + newTotal);
        }
        if (legacy.searchByName("ITEM 99").size() != refactored.searchByName("ITEM 99").size()) {
            throw new IllegalStateException("Search results differ");
        }

        System.out.printf("Inventory size: %,d items%n%n", ITEMS);
        System.out.printf("%-34s %12s %12s %10s%n", "Operation", "Before (ms)", "After (ms)", "Speed-up");

        report("getTotalInventoryValue x" + TOTAL_CALLS,
                () -> repeat(TOTAL_CALLS, () -> (int) legacy.getTotalInventoryValue()),
                () -> repeat(TOTAL_CALLS, () -> (int) refactored.getTotalInventoryValue()));

        report("searchByName(\"ITEM 99\") x" + SEARCH_CALLS,
                () -> repeat(SEARCH_CALLS, () -> legacy.searchByName("ITEM 99").size()),
                () -> repeat(SEARCH_CALLS, () -> refactored.searchByName("ITEM 99").size()));

        System.out.println("\n(checksum " + sink + ")");
    }

    private static int repeat(int times, IntSupplier call) {
        int acc = 0;
        for (int i = 0; i < times; i++) {
            acc += call.getAsInt();
        }
        return acc;
    }

    private static void report(String label, IntSupplier before, IntSupplier after) {
        double beforeMs = medianMillis(before);
        double afterMs = medianMillis(after);
        System.out.printf("%-34s %12.2f %12.2f %9.1fx%n", label, beforeMs, afterMs, beforeMs / afterMs);
    }

    private static double medianMillis(IntSupplier task) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            sink += task.getAsInt();
        }
        double[] times = new double[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            sink += task.getAsInt();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        Arrays.sort(times);
        return times[MEASURED_RUNS / 2];
    }
}
