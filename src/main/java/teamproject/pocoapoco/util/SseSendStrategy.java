package teamproject.pocoapoco.util;

import lombok.Builder;
import org.springframework.context.annotation.Configuration;

import static teamproject.pocoapoco.controller.main.api.sse.SseController.sseEmitters;

@Configuration
public interface SseSendStrategy {
    public default boolean isUserLogin(final String username) {
        return sseEmitters.containsKey(username);
    }

    public void SendAlarm(String userSseKey, String fromUser, String target, String message);
}
