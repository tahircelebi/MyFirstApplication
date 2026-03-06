package mypackage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JokeApiService implements JokeService {
    private static final String JOKE_API_URL = "https://v2.jokeapi.dev/joke/Programming";
    private static final String FALLBACK_STATUS_JOKE = "I would tell you a UDP joke, but you might not get it.";
    private static final String FALLBACK_DEFAULT_JOKE = "There are only 10 kinds of people in the world: those who understand binary and those who do not.";

    private final HttpClient httpClient;
    private final AppLogger logger;

    public JokeApiService(HttpClient httpClient, AppLogger logger) {
        this.httpClient = httpClient;
        this.logger = logger;
    }

    @Override
    public String fetchProgrammingJoke() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(JOKE_API_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("JOKE_API_RESPONSE", "status=" + response.statusCode() + " fallback=true");
                return FALLBACK_STATUS_JOKE;
            }

            String payload = response.body();
            String type = extractJsonString(payload, "type");
            if ("single".equals(type)) {
                String joke = extractJsonString(payload, "joke");
                if (joke != null && !joke.isBlank()) {
                    logger.info("JOKE_API_SUCCESS", "type=single length=" + joke.length());
                    return joke;
                }
            }

            if ("twopart".equals(type)) {
                String setup = extractJsonString(payload, "setup");
                String delivery = extractJsonString(payload, "delivery");
                if (setup != null && delivery != null) {
                    logger.info("JOKE_API_SUCCESS", "type=twopart length=" + (setup.length() + delivery.length() + 1));
                    return setup + " " + delivery;
                }
            }
        } catch (Exception ex) {
            logger.error("JOKE_API_EXCEPTION", "exception=" + ex.getClass().getSimpleName() + " message=" + ex.getMessage());
        }

        logger.warn("JOKE_FALLBACK_USED", "reason=unusable_api_payload");
        return FALLBACK_DEFAULT_JOKE;
    }

    private String extractJsonString(String json, String key) {
        String regex = "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\f", "\f");
    }
}
