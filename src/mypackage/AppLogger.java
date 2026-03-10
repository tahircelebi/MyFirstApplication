package mypackage;

/**
 * Minimal structured logger used to keep route and integration logging consistent.
 */
public interface AppLogger {
    void info(String event, String details);

    void warn(String event, String details);

    void error(String event, String details);
}
