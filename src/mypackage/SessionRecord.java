package mypackage;

import java.time.OffsetDateTime;

/**
 * Immutable representation of the one active authenticated session.
 */
public record SessionRecord(String token, String username, OffsetDateTime createdAt) {
}
