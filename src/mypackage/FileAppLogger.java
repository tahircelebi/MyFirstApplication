package mypackage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public class FileAppLogger implements AppLogger {
    private final Path logPath;

    public FileAppLogger(Path logPath) {
        this.logPath = logPath;
    }

    @Override
    public void info(String event, String details) {
        write("INFO", event, details);
    }

    @Override
    public void warn(String event, String details) {
        write("WARN", event, details);
    }

    @Override
    public void error(String event, String details) {
        write("ERROR", event, details);
    }

    private synchronized void write(String level, String event, String details) {
        String line = OffsetDateTime.now()
                + "|"
                + sanitize(level)
                + "|"
                + sanitize(event)
                + "|"
                + sanitize(details)
                + System.lineSeparator();
        try {
            Files.writeString(logPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            System.err.println("Failed to write application.log: " + ex.getMessage());
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("|", "/").replace("\n", " ").replace("\r", " ").trim();
    }
}
