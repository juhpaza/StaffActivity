package fi.juhpaza.staffactivity.command;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts safe command roots for counting without storing command arguments.
 */
public final class CommandRootExtractor {
    private CommandRootExtractor() {
    }

    public static Optional<String> extractAllowedRoot(String message, Set<String> excludedCommands) {
        String normalized = message == null ? "" : message.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String root = normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int namespaceSeparator = root.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < root.length()) {
            root = root.substring(namespaceSeparator + 1);
        }
        if (excludedCommands.contains(root)) {
            return Optional.empty();
        }
        return Optional.of(root);
    }
}
