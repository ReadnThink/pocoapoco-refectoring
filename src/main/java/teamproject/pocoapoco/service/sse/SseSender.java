package teamproject.pocoapoco.service.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import teamproject.pocoapoco.service.sse.dto.SseAlarmData;

import static teamproject.pocoapoco.controller.main.api.sse.SseController.sseEmitters;

@Component
public class SseSender {
    public void sendAlarmFromUserToTargetUser(final UserSseKey userSseKey, final SseAlarmData data) {
        SseEmitter userSseEmitter = getUserEmitter(userSseKey.getUserSseKey());
        try {
            userSseEmitter
                    .send(SseEmitter
                            .event()
                            .name(data.getAlarmTypeEnum().getValue())
                            .data(
                                    data.getFromUser()
                                            + "님이 \""
                                            + data.getTargetUser()
                                            + data.getMessage().getValue()));
        } catch (Exception e) {
            sseEmitters.remove(userSseKey);
        }
    }

    public void sendAlarmOnlyData(final UserSseKey userSseKey, final SseAlarmData data) {
        SseEmitter userSseEmitter = getUserEmitter(userSseKey.getUserSseKey());
        try {
            userSseEmitter
                    .send(SseEmitter
                            .event()
                            .name(data.getAlarmTypeEnum().getValue())
                            .data(data.getMessage().getValue()));
        } catch (Exception e) {
            sseEmitters.remove(userSseKey);
        }
    }

    public void sendAlarmToTargetUser(final UserSseKey userSseKey, final SseAlarmData data) {
        SseEmitter userSseEmitter = getUserEmitter(userSseKey.getUserSseKey());
        try {
            userSseEmitter
                    .send(SseEmitter
                            .event()
                            .name(data.getAlarmTypeEnum().getValue())
                            .data(data.getFromUser() + data.getMessage().getValue()));
        } catch (Exception e) {
            sseEmitters.remove(userSseKey);
        }
    }

    public void sendAlarmRandomMatch(final UserSseKey userSseKey, final SseAlarmData data) {
        SseEmitter userSseEmitter = getUserEmitter(userSseKey.getUserSseKey());
        try {
            userSseEmitter
                    .send(SseEmitter
                            .event()
                            .name(data.getAlarmTypeEnum().getValue())
                            .data(data.getData()));
        } catch (Exception e) {
            sseEmitters.remove(userSseKey);
        }
    }

    private static SseEmitter getUserEmitter(final String userSseKey) {
        return sseEmitters.get(userSseKey);
    }
}
