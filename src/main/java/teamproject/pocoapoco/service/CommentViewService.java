package teamproject.pocoapoco.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import teamproject.pocoapoco.domain.dto.comment.CommentRequest;
import teamproject.pocoapoco.domain.dto.comment.ui.CommentViewResponse;
import teamproject.pocoapoco.domain.entity.Alarm;
import teamproject.pocoapoco.domain.entity.Comment;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.enums.AlarmType;
import teamproject.pocoapoco.exception.AppException;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.repository.AlarmRepository;
import teamproject.pocoapoco.repository.CommentRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.service.sse.dto.SseAlarmData;
import teamproject.pocoapoco.service.sse.SseSender;
import teamproject.pocoapoco.service.sse.UserSseKey;

import java.util.List;

import static teamproject.pocoapoco.service.sse.dto.AlarmMessagesEnum.ADD_COMMENT_TO_COMMENT;

@Service
@Transactional
@Slf4j
public class CommentViewService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final SseSender sseSender;

    public CommentViewService(final CommentRepository commentRepository,
                              final UserRepository userRepository,
                              final AlarmRepository alarmRepository,
                              final SseSender sseSender) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.alarmRepository = alarmRepository;
        this.sseSender = sseSender;
    }
    @Transactional(readOnly = true)
    public List<CommentViewResponse> getCommentViewList(Long crewId) {
        List<Comment> list = commentRepository.findByCrewId(crewId);
        return Comment.from(list);
    }
    @Transactional(readOnly = true)
    public List<CommentViewResponse> getChildCommentViewList(Long crewId, Long parentCommentId) {
        List<Comment> list = commentRepository.findByCrewIdAndParentId(crewId, parentCommentId);
        return Comment.from(list);
    }

    // 대댓글 작성 - view
    public CommentViewResponse addChildComment(CommentRequest commentRequest, Long parentCommentId, String userName) {
        User user = getUser(userName);
        Comment parentComment = getComment(parentCommentId);

        Comment comment = commentRepository.save(commentRequest.toEntity(user, parentComment.getCrew()));
        comment.setParent(parentComment);
        List<Comment> list = parentComment.getChildren();
        list.add(comment);
        parentComment.setChildren(list);
        alarmRepository.save(Alarm.toEntityFromComment(user, parentComment.getCrew(),parentComment ,AlarmType.ADD_COMMENT, comment.getComment()));

        sendSseAlarm(user, parentComment);

        return CommentViewResponse.of(comment);
    }

    private void sendSseAlarm(final User user, final Comment parentComment) {
        sseSender.sendAlarmFromUserToTargetUser(
                UserSseKey.builder()
                        .userSseKey(parentComment.getUser().getUsername())
                        .build(),
                SseAlarmData.builder()
                        .fromUser(user.getNickName())
                        .targetUser(parentComment.getComment())
                        .message(ADD_COMMENT_TO_COMMENT)
                        .build()
        );
    }

    private User getUser(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USERID_NOT_FOUND, ErrorCode.USERID_NOT_FOUND.getMessage()));
    }
    private Comment getComment(Long parentCommentId) {
        return commentRepository.findById(parentCommentId).orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND.getMessage()));
    }


}