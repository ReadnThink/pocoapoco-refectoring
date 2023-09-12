package teamproject.pocoapoco.service.sse.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SseAlarmData {
    private final String fromUser;
    private final String targetUser;
    private final AlarmMessagesEnum message;

    @Builder
    public SseAlarmData(final String fromUser, final String targetUser, final AlarmMessagesEnum message) {
        this.fromUser = fromUser;
        this.targetUser = targetUser;
        this.message = message;
    }
}
