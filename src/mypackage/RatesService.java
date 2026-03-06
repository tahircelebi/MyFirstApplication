package mypackage;

/**
 * Provides exchange-rate retrieval and JSON serialization for HTTP responses.
 */
public interface RatesService {
    /**
     * Fetches latest rates from the configured provider.
     *
     * @return rates object, optionally stale/cached when provider fails
     */
    TcmbRates fetchRates();

    /**
     * Serializes rate data for API responses.
     *
     * @param rates rate payload
     * @return json string
     */
    String toJson(TcmbRates rates);
}
