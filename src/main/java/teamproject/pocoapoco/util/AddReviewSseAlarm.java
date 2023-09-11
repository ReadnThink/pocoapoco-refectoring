package teamproject.pocoapoco.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static teamproject.pocoapoco.controller.main.api.sse.SseController.sseEmitters;

@Slf4j
@Configuration
public class AddReviewSseAlarm implements SseSendStrategy {
    @Override
    public void SendAlarm(final String userSseKey, final String fromUser, final String target, final String message) {
        log.info("모임 종료 후 작동");
        SseEmitter userSseEmitter = sseEmitters.get(userSseKey);
        try {
            userSseEmitter
                    .send(SseEmitter
                            .event()
                            .name("alarm")
                            .data(message));
        } catch (Exception e) {
            sseEmitters.remove(userSseKey);
        }
    }
}
