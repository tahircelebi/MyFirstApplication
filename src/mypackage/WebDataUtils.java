package mypackage;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class WebDataUtils {
    private WebDataUtils() {
    }

    public static Map<String, String> parseFormData(String body) {
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return result;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    public static String extractCookieValue(String cookieHeader, String cookieName) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] cookies = cookieHeader.split(";");
        String prefix = cookieName + "=";
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length());
            }
        }
        return null;
    }

    public static String formatDisplayName(String username) {
        if (username == null || username.isBlank()) {
            return "User";
        }
        String trimmed = username.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
