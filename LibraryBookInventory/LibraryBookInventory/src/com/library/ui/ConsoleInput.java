package com.library.ui;

import com.library.exception.InvalidInputException;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Small helper that reads from the keyboard and keeps asking until the answer is valid.
 * Keeping this separate from the menu means the menu code reads like a script of the dialogue.
 */
public class ConsoleInput {

    /** Thrown when the input stream ends (Ctrl+D / Ctrl+Z, or piped input runs out). */
    public static class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) { this.scanner = scanner; }

    /** Prints the prompt and returns one trimmed line of text. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new EndOfInputException();    // nothing more to read: let the menu exit cleanly
        }
        return scanner.nextLine().trim();
    }

    /**
     * Asks repeatedly until {@code validator} accepts the answer. The validator converts the text
     * into the cleaned value (for example "978-0-13-468599-1" into "9780134685991") or throws
     * InvalidInputException, whose message is shown before asking again.
     */
    public <T> T readValid(String prompt, Function<String, T> validator) {
        while (true) {
            String answer = readLine(prompt);
            try {
                return validator.apply(answer);
            } catch (InvalidInputException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    /**
     * Like {@link #readValid} but an empty answer is allowed and returned as an empty string,
     * meaning "keep the current value" in the update dialogue.
     */
    public String readOptional(String prompt, Function<String, ?> validator) {
        while (true) {
            String answer = readLine(prompt);
            if (answer.isEmpty()) {
                return "";
            }
            try {
                validator.apply(answer);
                return answer;
            } catch (InvalidInputException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    /** Reads a menu choice. Any non-number gives -1, which the menu treats as "invalid option". */
    public int readMenuChoice(String prompt) {
        String answer = readLine(prompt);
        try {
            return Integer.parseInt(answer);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Asks a yes/no question; only "y" or "yes" (any case) counts as yes. */
    public boolean confirm(String prompt) {
        String answer = readLine(prompt + " (y/n): ").toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }
}
