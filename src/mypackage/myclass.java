package mypackage;

import org.apache.camel.main.Main;

import java.net.http.HttpClient;
import java.nio.file.Path;

/**
 * Application entry point and dependency composition root.
 */
public class myclass {
    /**
     * Boots Camel routes with concrete service implementations.
     *
     * @param args process arguments
     * @throws Exception startup exception
     */
    public static void main(String... args) throws Exception {
        AppLogger logger = new FileAppLogger(Path.of("application.log"));
        HttpClient httpClient = HttpClient.newBuilder().build();

        // Compose the application with interface-based services (SOLID: dependency inversion).
        SessionManager sessionManager = new InMemorySingleSessionManager();
        JokeService jokeService = new JokeApiService(httpClient, logger);
        RatesService ratesService = new InvestingRatesService(httpClient, logger);
        PageRenderer pageRenderer = new DefaultPageRenderer();

        Main main = new Main();
        main.configure().addRoutesBuilder(
                new LoginRoutes(sessionManager, jokeService, ratesService, pageRenderer, logger)
        );

        System.out.println("Camel server is running at:");
        System.out.println("http://localhost:8080/login");
        logger.info("APP_START", "server_url=http://localhost:8080/login");

        main.run(args);
    }
}
