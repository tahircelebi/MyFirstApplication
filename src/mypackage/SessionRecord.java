package mypackage;

import java.time.OffsetDateTime;

public record SessionRecord(String token, String username, OffsetDateTime createdAt) {
}
