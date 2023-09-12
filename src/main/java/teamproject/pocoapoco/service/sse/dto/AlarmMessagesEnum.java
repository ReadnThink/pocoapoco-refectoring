package teamproject.pocoapoco.service.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AlarmMessagesEnum {
    CHECK_REVIEW("리뷰가 등록되었어요! 확인해 보세요!"),
    ADD_COMMENT_TO_ACTIVITY("\"모임에 댓글을 남겼습니다."),
    ADD_COMMENT_TO_COMMENT("\" 댓글에 댓글을 남겼습니다."),
    ADD_REVIEW_TO_CREWS("모임이 종료되었습니다! 같이 고생한 크루들에게 후기를 남겨보세요!"),
    FOLLOW("님이 회원님을 팔로우 합니다💕"),
    LIKE("\" 모임에 좋아요를 눌렀습니다💕")
    ;

    private final String value;
}
