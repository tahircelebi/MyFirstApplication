package mypackage;

/**
 * Renders HTML pages for the authentication flow.
 */
public interface PageRenderer {
    /**
     * Renders login page.
     *
     * @param warning optional warning text
     * @return login page html
     */
    String renderLoginPage(String warning);

    /**
     * Renders welcome page.
     *
     * @param username current authenticated username
     * @param joke initial joke text
     * @return welcome page html
     */
    String renderWelcomePage(String username, String joke);
}
