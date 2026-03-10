package mypackage;

/**
 * Controls the application's single active authenticated session.
 */
public interface SessionManager {
    SessionRecord findByToken(String token);

    SessionRecord getActive();

    LoginResult login(String username, String sessionToken);

    boolean logout(String sessionToken);
}
