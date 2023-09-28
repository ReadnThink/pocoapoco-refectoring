package teamproject.pocoapoco.service.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AlarmTypeEnum {
    ALARM("alarm"),
    LIVE_MATCH("liveMatch"),
    LIVE_MATCH_WAITING_CNT("liveMatchCnt"),
    LIVE_MATCH_CANCEL("liveMatchCancel")
    ;

    private final String value;
}
