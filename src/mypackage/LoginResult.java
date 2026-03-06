package mypackage;

public record LoginResult(LoginOutcome outcome, SessionRecord session, String activeUsername) {
}
