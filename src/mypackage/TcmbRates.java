package mypackage;

/**
 * Rate payload returned to the UI, optionally marked stale when served from cache.
 */
public record TcmbRates(Double usdTry, Double sarTry, String sourceDate, boolean stale) {
}
