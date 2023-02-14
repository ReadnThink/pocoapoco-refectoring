package teamproject.pocoapoco.domain.dto.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import teamproject.pocoapoco.domain.entity.Alarm;
import teamproject.pocoapoco.enums.AlarmType;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class AlarmResponse {
    private Long id;
    private AlarmType alarmType;
    private String fromUserName;
    private String imagePath;
    private Long targetId;
    private String text;
    private LocalDateTime createdAt;
    private boolean readOrNot;

    public static AlarmResponse fromEntity(Alarm alarm) {
        return AlarmResponse.builder()
                .id(alarm.getId())
                .alarmType(AlarmType.LIKE_CREW)
                .fromUserName(alarm.getFromUserName())
                .targetId(alarm.getTargetCrewId())
                .text(alarm.getMassage())
                .createdAt(alarm.getCreatedAt())
                .readOrNot(alarm.getReadOrNot())
                .imagePath(alarm.getUser().getImagePath())
                .build();
    }
}

