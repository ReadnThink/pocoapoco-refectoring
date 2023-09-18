package teamproject.pocoapoco.service.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AlarmTypeEnum {
    ALARM("alarm");

    private final String value;
}
