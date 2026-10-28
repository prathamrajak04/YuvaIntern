package com.inventory.repository;

import com.inventory.model.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts an {@link Item} to and from one line of CSV.
 *
 * <p>PLACEMENT: this class lives in the repository package (not util) because the file format is a
 * storage concern. Keeping it here also removes a package cycle (model -> util -> model) that
 * appeared when it was first written in util.
 *
 * <p>Format: {@code id,name,quantity,price}. A field containing a comma or a double quote is wrapped
 * in double quotes and embedded quotes are doubled (RFC 4180 style):
 * <pre>
 *   A1,Laptop,10,50000.0
 *   A2,"Cable, USB-C",25,199.5
 *   A3,"15"" Monitor",4,8999.0
 * </pre>
 * Line breaks are not supported inside a field. {@link com.inventory.util.Validators#requireNonBlank} rejects them at
 * the model level, so every valid Item can be written as exactly one line.
 *
 * <p>Prices use {@code Double.toString}/{@code Double.parseDouble}, which are locale-independent,
 * so a file written on a machine using "1,5" decimals still reads correctly everywhere.
 */
final class CsvCodec {   // package-private: an implementation detail of the CSV repository

    public static final String HEADER = "id,name,quantity,price";

    private CsvCodec() {
        // utility class
    }

    public static String encode(Item item) {
        return escape(item.id()) + "," + escape(item.name()) + "," + item.quantity() + "," + item.price();
    }

    public static Item decode(String line) {
        List<String> fields = split(line);
        if (fields.size() != 4) {
            throw new IllegalArgumentException("Expected 4 fields but found " + fields.size());
        }
        try {
            return new Item(
                    fields.get(0),
                    fields.get(1),
                    Integer.parseInt(fields.get(2).trim()),
                    Double.parseDouble(fields.get(3).trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number (" + e.getMessage() + ")", e);
        }
    }

    static String escape(String field) {
        if (field.indexOf(',') >= 0 || field.indexOf('"') >= 0) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); // doubled quote = literal quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Unterminated quoted field");
        }
        fields.add(current.toString());
        return fields;
    }
}
