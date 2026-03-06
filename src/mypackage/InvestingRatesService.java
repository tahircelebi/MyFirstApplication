package mypackage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches USD/TRY and SAR/TRY rates from public Investing.com currency pages.
 */
public class InvestingRatesService implements RatesService {
    // Chart pages tend to expose the latest quote in server-rendered HTML more reliably.
    private static final String USD_TRY_URL = "https://www.investing.com/currencies/usd-try-chart";
    private static final String SAR_TRY_URL = "https://www.investing.com/currencies/sar-try-chart";

    // These patterns target commonly rendered price markers on Investing pages.
    private static final Pattern PRICE_DATA_TEST_PATTERN = Pattern.compile(
            "data-test=\\\"instrument-price-last\\\"[^>]*>([^<]+)<", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_FALLBACK_PATTERN = Pattern.compile(
            "\\\"last_last\\\"\\s*:\\s*\\\"([0-9.,]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_ALT_JSON_PATTERN = Pattern.compile(
            "\\\"last\\\"\\s*:\\s*\\\"([0-9.,]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_REALTIME_TEXT_PATTERN = Pattern.compile(
            "([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2,6}))\\s*(?:\\+|-)?[0-9.,]*\\s*\\([^)]*%\\)",
            Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;
    private final AppLogger logger;
    private TcmbRates lastKnownRates;

    /**
     * Creates an investing-backed rates service.
     *
     * @param httpClient shared HTTP client
     * @param logger application logger
     */
    public InvestingRatesService(HttpClient httpClient, AppLogger logger) {
        this.httpClient = httpClient;
        this.logger = logger;
    }

    @Override
    public synchronized TcmbRates fetchRates() {
        try {
            TcmbRates fresh = fetchFromInvesting();
            if (fresh.usdTry() != null && fresh.sarTry() != null) {
                lastKnownRates = fresh;
            }
            return fresh;
        } catch (Exception ex) {
            if (lastKnownRates != null) {
                logger.warn("INVESTING_RATES_FALLBACK",
                        "reason=fetch_error using_cached=true exception=" + ex.getClass().getSimpleName());
                return new TcmbRates(lastKnownRates.usdTry(), lastKnownRates.sarTry(), lastKnownRates.sourceDate(), true);
            }
            logger.error("INVESTING_RATES_ERROR",
                    "exception=" + ex.getClass().getSimpleName() + " message=" + safeText(ex.getMessage()));
            return new TcmbRates(null, null, null, false);
        }
    }

    @Override
    public String toJson(TcmbRates rates) {
        if (rates == null) {
            return "{\"usdTry\":null,\"sarTry\":null,\"sourceDate\":null,\"stale\":false}";
        }
        return "{"
                + "\"usdTry\":" + toJsonNumber(rates.usdTry()) + ","
                + "\"sarTry\":" + toJsonNumber(rates.sarTry()) + ","
                + "\"sourceDate\":" + toJsonString(rates.sourceDate()) + ","
                + "\"stale\":" + rates.stale()
                + "}";
    }

    private TcmbRates fetchFromInvesting() throws Exception {
        Double usdTry = fetchPairLastPrice(USD_TRY_URL, "USD_TRY");
        Double sarTry = fetchPairLastPrice(SAR_TRY_URL, "SAR_TRY");

        String sourceDate = OffsetDateTime.now().toString();
        logger.info("INVESTING_RATES_FETCH",
                "usd_try=" + safeNumber(usdTry) + " sar_try=" + safeNumber(sarTry) + " fetched_at=" + sourceDate);

        return new TcmbRates(usdTry, sarTry, sourceDate, false);
    }

    private Double fetchPairLastPrice(String url, String pairName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(pairName + " HTTP status " + response.statusCode());
        }

        String body = response.body();
        Double parsed = extractPrice(body);
        if (parsed == null) {
            logger.warn("INVESTING_PARSE_MISS",
                    "pair=" + pairName + " snippet=" + safeText(snippet(body)));
            throw new IllegalStateException("Could not parse " + pairName + " from investing page");
        }
        return parsed;
    }

    private Double extractPrice(String html) {
        Matcher primaryMatcher = PRICE_DATA_TEST_PATTERN.matcher(html);
        if (primaryMatcher.find()) {
            return parseFlexibleDecimal(primaryMatcher.group(1));
        }

        Matcher fallbackMatcher = PRICE_FALLBACK_PATTERN.matcher(html);
        if (fallbackMatcher.find()) {
            return parseFlexibleDecimal(fallbackMatcher.group(1));
        }

        Matcher altJsonMatcher = PRICE_ALT_JSON_PATTERN.matcher(html);
        if (altJsonMatcher.find()) {
            return parseFlexibleDecimal(altJsonMatcher.group(1));
        }

        Matcher realtimeMatcher = PRICE_REALTIME_TEXT_PATTERN.matcher(html);
        if (realtimeMatcher.find()) {
            return parseFlexibleDecimal(realtimeMatcher.group(1));
        }

        return null;
    }

    private Double parseFlexibleDecimal(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim().replace(" ", "").replace("\u00A0", "");
        if (cleaned.isEmpty()) {
            return null;
        }

        int lastComma = cleaned.lastIndexOf(',');
        int lastDot = cleaned.lastIndexOf('.');

        // Normalize locale-specific number formats before parsing as Java double.
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (lastComma >= 0) {
            cleaned = cleaned.replace(",", ".");
        }

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toJsonNumber(Double value) {
        if (value == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String safeNumber(Double value) {
        if (value == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private String safeText(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    /**
     * Returns a short sanitized snippet for diagnostics when parsing fails.
     */
    private String snippet(String value) {
        if (value == null || value.isBlank()) {
            return "empty";
        }
        String cleaned = value.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 180 ? cleaned : cleaned.substring(0, 180);
    }
}
