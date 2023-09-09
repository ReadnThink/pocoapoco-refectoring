package teamproject.pocoapoco.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import teamproject.pocoapoco.domain.entity.Comment;
import teamproject.pocoapoco.domain.entity.Crew;
import teamproject.pocoapoco.domain.entity.User;

import static teamproject.pocoapoco.controller.main.api.sse.SseController.sseEmitters;

@Slf4j
public class SseSender {
    public static boolean isUserLogin(final String username) {
        return sseEmitters.containsKey(username);
    }

    public static void SendAlarmToUser(final User user, final Crew crew, final String message) {
        SseEmitter sseEmitter = sseEmitters.get(crew.getUser().getUsername());
        try {
            sseEmitter
                    .send(SseEmitter.event().name("alarm")
                    .data(user.getNickName() + "님이 \"" + crew.getTitle() + message));

        } catch (Exception e) {
            sseEmitters.remove(crew.getUser().getUsername());
        }
    }

    public static void SendAlarmToUser(final User user, final Comment parentComment, final String message) {
        SseEmitter sseEmitter = sseEmitters.get(parentComment.getUser().getUsername());
        try {
            sseEmitter
                    .send(SseEmitter.event()
                    .name("alarm")
                    .data(user.getNickName() + "님이 \"" + parentComment.getComment() + message));
        } catch (Exception e) {
            sseEmitters.remove(parentComment.getUser().getUsername());
        }
    }

    public static void SendAlarmToUser(final User toUser, final String message) {
        SseEmitter sseEmitter = sseEmitters.get(toUser.getUsername());
        try {
            sseEmitter
                    .send(SseEmitter.event()
                    .name("alarm")
                    .data(message));
        } catch (Exception e) {
            sseEmitters.remove(toUser.getUsername());
        }
    }

    public static void SendAlarmToUser(final String toUser, final String message) {
        SseEmitter sseEmitter = sseEmitters.get(toUser);
        try {
            sseEmitter
                    .send(SseEmitter.event().name("alarm")
                    .data(message));
        } catch (Exception e) {
            sseEmitters.remove(toUser);
        }
    }

}
