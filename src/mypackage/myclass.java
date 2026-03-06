package mypackage;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class myclass {
    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new LoginRoutes());

        System.out.println("Camel server is running at:");
        System.out.println("http://localhost:8080/welcome");

        main.run(args);
    }

    static class LoginRoutes extends RouteBuilder {
        @Override
        public void configure() {
            // Redirect root path to the login page.
            from("jetty:http://0.0.0.0:8080/?httpMethodRestrict=GET")
                    .routeId("root-redirect")
                    .removeHeaders("*")
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(302))
                    .setHeader("Location", constant("/welcome"))
                    .setBody(constant(""));

            // Render login page on /welcome.
            from("jetty:http://0.0.0.0:8080/welcome?httpMethodRestrict=GET")
                    .routeId("welcome-page")
                    .removeHeaders("*")
                    .setHeader(Exchange.CONTENT_TYPE, constant("text/html; charset=UTF-8"))
                    .process(exchange -> exchange.getMessage().setBody(renderLoginPage()));

            // Support trailing-slash variant.
            from("jetty:http://0.0.0.0:8080/welcome/?httpMethodRestrict=GET")
                    .routeId("welcome-page-slash")
                    .removeHeaders("*")
                    .setHeader(Exchange.CONTENT_TYPE, constant("text/html; charset=UTF-8"))
                    .process(exchange -> exchange.getMessage().setBody(renderLoginPage()));

            // Handle login form submit and return either warning page or success page.
            from("jetty:http://0.0.0.0:8080/login?httpMethodRestrict=POST")
                    .routeId("login-submit")
                    .process(exchange -> {
                        String requestBody = exchange.getMessage().getBody(String.class);
                        Map<String, String> form = parseFormData(requestBody);

                        String username = form.getOrDefault("username", "");
                        String password = form.getOrDefault("password", "");

                        exchange.getMessage().removeHeaders("*");
                        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/html; charset=UTF-8");

                        // Authenticate only when username and password are identical.
                        if (!username.isBlank() && username.equals(password)) {
                            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                            exchange.getMessage().setBody(renderWelcomePage(formatDisplayName(username)));
                        } else {
                            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
                            exchange.getMessage().setBody(renderLoginPage("Invalid username or password. Please try again."));
                        }
                    });
        }
    }

    public static String renderLoginPage() {
        return renderLoginPage(null);
    }

    public static String renderLoginPage(String warning) {
        String warningHtml = "";
        if (warning != null && !warning.isEmpty()) {
            // Escape warning text before embedding into HTML.
            warningHtml = "<div class=\"warning\">" + escapeHtml(warning) + "</div>";
        }

        return "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                + "  <title>Welcome Login</title>\n"
                + "  <style>\n"
                + "    :root { --bg1:#fff8e8; --bg2:#f6e7c7; --accent:#c76b1f; --deep:#3a1f12; }\n"
                + "    * { box-sizing:border-box; }\n"
                + "    body { margin:0; font-family:Segoe UI, Arial, sans-serif; background:linear-gradient(145deg,var(--bg1),var(--bg2)); min-height:100vh; display:flex; align-items:center; justify-content:center; color:var(--deep); }\n"
                + "    .card { width:min(92vw,420px); background:#fff; border-radius:18px; box-shadow:0 16px 40px rgba(0,0,0,.14); overflow:hidden; border:1px solid rgba(199,107,31,.2); }\n"
                + "    .banner { padding:20px; background:linear-gradient(120deg,#f4a259,#f7d488); color:#2d1408; text-align:center; }\n"
                + "    .banner h1 { margin:0; font-size:26px; letter-spacing:.4px; }\n"
                + "    .banner p { margin:6px 0 0; opacity:.9; }\n"
                + "    .body { padding:20px; }\n"
                + "    label { display:block; font-weight:600; margin:10px 0 6px; }\n"
                + "    input { width:100%; padding:11px 12px; border:1px solid #d9c8af; border-radius:10px; font-size:14px; }\n"
                + "    input:focus { outline:none; border-color:var(--accent); box-shadow:0 0 0 3px rgba(199,107,31,.15); }\n"
                + "    button { margin-top:14px; width:100%; border:0; border-radius:10px; background:var(--accent); color:#fff; font-weight:700; padding:11px; cursor:pointer; }\n"
                + "    button:hover { filter:brightness(0.96); }\n"
                + "    .warning { margin-bottom:10px; border:1px solid #ef8c8c; background:#fff3f3; color:#8d1d1d; padding:10px 12px; border-radius:10px; }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <main class=\"card\">\n"
                + "    <section class=\"banner\">\n"
                + "      <h1>Welcome Portal</h1>\n"
                + "      <p>🐅 Tiger Access Panel</p>\n"
                + "    </section>\n"
                + "    <section class=\"body\">\n"
                + warningHtml
                + "      <form method=\"post\" action=\"/login\">\n"
                + "        <label for=\"username\">Username</label>\n"
                + "        <input id=\"username\" name=\"username\" type=\"text\" autocomplete=\"username\" required>\n"
                + "        <label for=\"password\">Password</label>\n"
                + "        <input id=\"password\" name=\"password\" type=\"password\" autocomplete=\"current-password\" required>\n"
                + "        <button type=\"submit\">Sign In</button>\n"
                + "      </form>\n"
                + "    </section>\n"
                + "  </main>\n"
                + "</body>\n"
                + "</html>\n";
    }

    private static String renderWelcomePage(String username) {
        BannerTheme theme = pickBannerTheme(username);
        return "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Welcome</title>\n"
                + "<style>body{margin:0;font-family:Segoe UI,Arial,sans-serif;min-height:100vh;display:grid;place-items:center;background:linear-gradient(145deg,#fff7e0,#f7ddb5);} .box{width:min(92vw,460px);background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 16px 40px rgba(0,0,0,.13);text-align:center;} .banner{padding:16px 20px;background:"
                + theme.gradient + ";color:#fff;} .banner h2{margin:0;font-size:22px;} .banner p{margin:6px 0 0;opacity:.95;} .content{padding:24px 30px;} h1{margin:0 0 8px;color:#48200f;} .msg{margin:0;color:#6b3b24;}</style>\n"
                + "</head>\n"
                + "<body><div class=\"box\"><div class=\"banner\"><h2>" + theme.title + "</h2><p>" + theme.icon
                + " Personalized access</p></div><div class=\"content\"><h1>Hello " + escapeHtml(username)
                + "</h1><p class=\"msg\">Welcome to your portal.</p></div></div></body>\n"
                + "</html>\n";
    }

    private static BannerTheme pickBannerTheme(String username) {
        int index = Math.abs(username.toLowerCase().hashCode()) % 4;
        return switch (index) {
            case 0 -> new BannerTheme("linear-gradient(135deg,#1b4332,#2d6a4f)", "Forest Gate", "🌿");
            case 1 -> new BannerTheme("linear-gradient(135deg,#003049,#669bbc)", "Ocean Deck", "🌊");
            case 2 -> new BannerTheme("linear-gradient(135deg,#4a1942,#9d4edd)", "Neon Vault", "⚡");
            default -> new BannerTheme("linear-gradient(135deg,#7f5539,#ddb892)", "Amber Hall", "🔥");
        };
    }

    private static String formatDisplayName(String username) {
        if (username == null || username.isBlank()) {
            return "User";
        }
        String trimmed = username.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    private static Map<String, String> parseFormData(String body) {
        // Parse URL-encoded form payload like "username=a&password=b".
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

    private static String escapeHtml(String text) {
        // Minimal escaping for reflected content.
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record BannerTheme(String gradient, String title, String icon) {
    }
}
