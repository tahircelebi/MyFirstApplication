package mypackage;

/**
 * Provides jokes that can be rendered on the welcome page.
 */
public interface JokeService {
    /**
     * Fetches a programming joke.
     *
     * @return joke text
     */
    String fetchProgrammingJoke();
}
