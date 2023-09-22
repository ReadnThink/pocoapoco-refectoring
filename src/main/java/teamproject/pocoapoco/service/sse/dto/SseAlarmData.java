package teamproject.pocoapoco.service.sse.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SseAlarmData {
    private final String fromUser;
    private final String targetUser;
    private String data;
    private final AlarmMessagesEnum message;
    private final AlarmTypeEnum alarmTypeEnum;

    @Builder
    public SseAlarmData(final String fromUser, final String targetUser, final String data, final AlarmMessagesEnum message, final AlarmTypeEnum alarmTypeEnum) {
        this.fromUser = fromUser;
        this.targetUser = targetUser;
        this.data = data;
        this.message = message;
        this.alarmTypeEnum = alarmTypeEnum;
    }
}
