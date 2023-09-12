package teamproject.pocoapoco.service.sse;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserSseKey {
    private final String userSseKey;

    @Builder
    public UserSseKey(final String userSseKey) {
        this.userSseKey = userSseKey;
    }
}
