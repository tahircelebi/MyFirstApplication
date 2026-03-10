package mypackage;

/**
 * Outcomes produced when a login attempt is evaluated against the single-session policy.
 */
public enum LoginOutcome {
    ALREADY_AUTHENTICATED,
    REJOINED,
    CREATED,
    BLOCKED
}
