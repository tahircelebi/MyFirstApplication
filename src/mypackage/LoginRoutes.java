package mypackage;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import java.util.Map;

/**
 * Defines HTTP routes for authentication, session checks, rates+joke data and logout.
 */
public class LoginRoutes extends RouteBuilder {
    private static final String AUTH_COOKIE_NAME = "AUTH_SESSION";

    private final SessionManager sessionManager;
    private final JokeService jokeService;
    private final RatesService ratesService;
    private final PageRenderer pageRenderer;
    private final AppLogger logger;

    /**
     * Creates route handlers with interface-driven dependencies.
     *
     * @param sessionManager single-session auth manager
     * @param jokeService joke provider service
     * @param ratesService currency rates provider service
     * @param pageRenderer html page renderer
     * @param logger structured application logger
     */
    public LoginRoutes(SessionManager sessionManager,
                       JokeService jokeService,
                       RatesService ratesService,
                       PageRenderer pageRenderer,
                       AppLogger logger) {
        this.sessionManager = sessionManager;
        this.jokeService = jokeService;
        this.ratesService = ratesService;
        this.pageRenderer = pageRenderer;
        this.logger = logger;
    }

    /**
     * Declares all HTTP routes for login, welcome, rates, session status and logout.
     */
    @Override
    public void configure() {
        onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String name = ex == null ? "unknown" : ex.getClass().getSimpleName();
                    String message = ex == null ? "n/a" : ex.getMessage();
                    logger.error("ROUTE_EXCEPTION", "route=" + safe(exchange.getFromRouteId())
                            + " exception=" + safe(name)
                            + " message=" + safe(message));
                })
                .handled(false);

        from("jetty:http://0.0.0.0:8080/?httpMethodRestrict=GET")
                .routeId("root-redirect")
                .process(exchange -> {
                    logger.info("ROOT_REDIRECT", "from=/ to=/login remote="
                            + safe(exchange.getMessage().getHeader("CamelHttpRemoteAddress", String.class)));
                    redirect(exchange, "/login");
                });

        from("jetty:http://0.0.0.0:8080/login?httpMethodRestrict=GET")
                .routeId("login-page")
                .process(this::handleLoginPageRequest);

        from("jetty:http://0.0.0.0:8080/login/?httpMethodRestrict=GET")
                .routeId("login-page-slash")
                .process(this::handleLoginPageRequest);

        from("jetty:http://0.0.0.0:8080/login?httpMethodRestrict=POST")
                .routeId("login-submit")
                .process(this::handleLoginSubmit);

        from("jetty:http://0.0.0.0:8080/welcome?httpMethodRestrict=GET")
                .routeId("welcome-page")
                .process(exchange -> handleWelcome(exchange, "/welcome"));

        from("jetty:http://0.0.0.0:8080/welcome/?httpMethodRestrict=GET")
                .routeId("welcome-page-slash")
                .process(exchange -> handleWelcome(exchange, "/welcome/"));

        from("jetty:http://0.0.0.0:8080/session-status?httpMethodRestrict=GET")
                .routeId("session-status")
                .process(this::handleSessionStatus);

        from("jetty:http://0.0.0.0:8080/rates?httpMethodRestrict=GET")
                .routeId("rates")
                .process(this::handleRates);

        from("jetty:http://0.0.0.0:8080/logout?httpMethodRestrict=POST")
                .routeId("logout")
                .process(this::handleLogout);

        from("jetty:http://0.0.0.0:8080/logout/?httpMethodRestrict=POST")
                .routeId("logout-slash")
                .process(this::handleLogout);
    }

    private void handleLoginPageRequest(Exchange exchange) {
        String token = extractAuthToken(exchange);
        SessionRecord session = sessionManager.findByToken(token);
        if (session != null) {
            logger.info("LOGIN_REDIRECT_ACTIVE_SESSION", "path=/login user=" + safe(session.username()));
            redirect(exchange, "/welcome");
            return;
        }

        logger.info("LOGIN_PAGE_VIEW", "path=/login");
        htmlResponse(exchange, 200, pageRenderer.renderLoginPage(null));
    }

    private void handleLoginSubmit(Exchange exchange) {
        String requestBody = exchange.getMessage().getBody(String.class);
        Map<String, String> form = WebDataUtils.parseFormData(requestBody);

        String username = form.getOrDefault("username", "");
        String password = form.getOrDefault("password", "");
        String token = extractAuthToken(exchange);

        if (username.isBlank() || !username.equals(password)) {
            logger.warn("LOGIN_FAILED", "username=" + safe(username) + " reason=invalid_credentials");
            htmlResponse(exchange, 401, pageRenderer.renderLoginPage("Invalid username or password. Please try again."));
            return;
        }

        String displayName = WebDataUtils.formatDisplayName(username);
        LoginResult result = sessionManager.login(displayName, token);
        if (result.outcome() == LoginOutcome.BLOCKED) {
            logger.warn("LOGIN_RESTRICTED_ACTIVE_SESSION", "attempted_username=" + safe(displayName)
                    + " active_username=" + safe(result.activeUsername()));
            htmlResponse(exchange, 409, pageRenderer.renderLoginPage("Another user is already logged in. Please try again later."));
            return;
        }

        if (result.outcome() == LoginOutcome.ALREADY_AUTHENTICATED) {
            logger.info("LOGIN_ALREADY_AUTHENTICATED", "username=" + safe(displayName) + " redirect=/welcome");
            redirect(exchange, "/welcome");
            return;
        }

        if (result.outcome() == LoginOutcome.REJOINED) {
            logger.info("LOGIN_REJOIN_ACTIVE_SESSION", "username=" + safe(displayName) + " redirect=/welcome");
        } else {
            logger.info("LOGIN_SUCCESS", "username=" + safe(displayName) + " redirect=/welcome");
        }

        exchange.getMessage().removeHeaders("*");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 302);
        exchange.getMessage().setHeader("Set-Cookie", buildAuthCookie(result.session().token()));
        exchange.getMessage().setHeader("Location", "/welcome");
        exchange.getMessage().setBody("");
    }

    private void handleWelcome(Exchange exchange, String path) {
        SessionRecord session = sessionManager.findByToken(extractAuthToken(exchange));
        if (session == null) {
            logger.warn("WELCOME_ACCESS_DENIED", "path=" + path + " reason=unauthorized_session");
            redirect(exchange, "/login");
            return;
        }

        String joke = jokeService.fetchProgrammingJoke();
        logger.info("WELCOME_PAGE_VIEW", "path=" + path + " user=" + safe(session.username()) + " joke_length=" + joke.length());
        htmlResponse(exchange, 200, pageRenderer.renderWelcomePage(session.username(), joke));
    }

    private void handleSessionStatus(Exchange exchange) {
        SessionRecord session = sessionManager.findByToken(extractAuthToken(exchange));
        exchange.getMessage().removeHeaders("*");
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/plain; charset=UTF-8");
        if (session == null) {
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
            exchange.getMessage().setBody("inactive");
            return;
        }
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        exchange.getMessage().setBody("active");
    }

    private void handleRates(Exchange exchange) {
        SessionRecord session = sessionManager.findByToken(extractAuthToken(exchange));
        exchange.getMessage().removeHeaders("*");
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json; charset=UTF-8");
        exchange.getMessage().setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        exchange.getMessage().setHeader("Pragma", "no-cache");
        exchange.getMessage().setHeader("Expires", "0");
        if (session == null) {
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
            exchange.getMessage().setBody("{\"error\":\"unauthorized\"}");
            return;
        }

        TcmbRates rates = ratesService.fetchRates();
        String joke = jokeService.fetchProgrammingJoke();
        int status = rates.usdTry() != null && rates.sarTry() != null ? 200 : 503;
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, status);
        exchange.getMessage().setBody(buildRatesAndJokeJson(rates, joke));
    }

    private void handleLogout(Exchange exchange) {
        String token = extractAuthToken(exchange);
        SessionRecord session = sessionManager.findByToken(token);
        boolean cleared = sessionManager.logout(token);
        String username = session == null ? "unknown" : session.username();
        logger.info("LOGOUT", "username=" + safe(username) + " success=" + cleared);

        exchange.getMessage().removeHeaders("*");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 302);
        exchange.getMessage().setHeader("Set-Cookie", clearAuthCookie());
        exchange.getMessage().setHeader("Location", "/login");
        exchange.getMessage().setBody("");
    }

    private void htmlResponse(Exchange exchange, int statusCode, String html) {
        exchange.getMessage().removeHeaders("*");
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/html; charset=UTF-8");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getMessage().setBody(html);
    }

    private void redirect(Exchange exchange, String location) {
        exchange.getMessage().removeHeaders("*");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 302);
        exchange.getMessage().setHeader("Location", location);
        exchange.getMessage().setBody("");
    }

    private String extractAuthToken(Exchange exchange) {
        // A single HttpOnly auth cookie represents the one allowed active session.
        return WebDataUtils.extractCookieValue(exchange.getMessage().getHeader("Cookie", String.class), AUTH_COOKIE_NAME);
    }

    /**
     * Combines rates JSON and current joke into a single payload so UI can refresh together.
     */
    private String buildRatesAndJokeJson(TcmbRates rates, String joke) {
        String ratesJson = ratesService.toJson(rates);
        if (ratesJson.endsWith("}")) {
            return ratesJson.substring(0, ratesJson.length() - 1)
                    + ",\"joke\":\"" + escapeJson(joke) + "\"}";
        }
        return "{\"usdTry\":null,\"sarTry\":null,\"sourceDate\":null,\"stale\":false,\"joke\":\""
                + escapeJson(joke) + "\"}";
    }

    /**
     * Escapes text for safe embedding in JSON string values.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String buildAuthCookie(String token) {
        return AUTH_COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; SameSite=Lax";
    }

    private String clearAuthCookie() {
        return AUTH_COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
    }

    private String safe(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }
}
