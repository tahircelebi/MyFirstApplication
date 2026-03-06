package mypackage;

import java.time.OffsetDateTime;
import java.util.UUID;

public class InMemorySingleSessionManager implements SessionManager {
    private SessionRecord activeSession;

    @Override
    public synchronized SessionRecord findByToken(String token) {
        if (token == null || activeSession == null) {
            return null;
        }
        if (!activeSession.token().equals(token)) {
            return null;
        }
        return activeSession;
    }

    @Override
    public synchronized SessionRecord getActive() {
        return activeSession;
    }

    @Override
    public synchronized LoginResult login(String username, String sessionToken) {
        SessionRecord byToken = findByToken(sessionToken);
        if (byToken != null) {
            return new LoginResult(LoginOutcome.ALREADY_AUTHENTICATED, byToken, byToken.username());
        }

        if (activeSession != null) {
            if (activeSession.username().equals(username)) {
                return new LoginResult(LoginOutcome.REJOINED, activeSession, activeSession.username());
            }
            return new LoginResult(LoginOutcome.BLOCKED, null, activeSession.username());
        }

        activeSession = new SessionRecord(UUID.randomUUID().toString(), username, OffsetDateTime.now());
        return new LoginResult(LoginOutcome.CREATED, activeSession, activeSession.username());
    }

    @Override
    public synchronized boolean logout(String sessionToken) {
        if (sessionToken == null || activeSession == null) {
            return false;
        }
        if (!activeSession.token().equals(sessionToken)) {
            return false;
        }
        activeSession = null;
        return true;
    }
}
