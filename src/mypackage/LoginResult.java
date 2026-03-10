package mypackage;

/**
 * Result of a login attempt, including the session when one is available and the active username for messaging.
 */
public record LoginResult(LoginOutcome outcome, SessionRecord session, String activeUsername) {
}
