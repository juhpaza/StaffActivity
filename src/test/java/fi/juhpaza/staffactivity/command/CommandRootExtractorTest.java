package fi.juhpaza.staffactivity.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class CommandRootExtractorTest {
    @Test
    void extractsRootWithoutArguments() {
        assertEquals("ban", CommandRootExtractor.extractAllowedRoot("/ban Player reason", Set.of()).orElseThrow());
    }

    @Test
    void stripsNamespace() {
        assertEquals("ban", CommandRootExtractor.extractAllowedRoot("/essentials:ban Player", Set.of()).orElseThrow());
    }

    @Test
    void excludesSensitiveCommands() {
        assertTrue(CommandRootExtractor.extractAllowedRoot("/login hunter2", Set.of("login")).isEmpty());
    }

    @Test
    void ignoresBlankCommands() {
        assertTrue(CommandRootExtractor.extractAllowedRoot("/", Set.of()).isEmpty());
    }
}
