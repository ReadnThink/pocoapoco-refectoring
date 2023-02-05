package teamproject.pocoapoco.domain.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import teamproject.pocoapoco.domain.entity.Comment;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class CommentResponse {
    private Long id;
    private String comment;
    private String userName;
    private Long crewId;
    private boolean isParent;
    private boolean isDeleted;
    private List<CommentResponse> children;
    private LocalDateTime createdAt;

    public static CommentResponse of(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .comment(comment.getComment())
                .userName(comment.getUser().getUsername())
                .crewId(comment.getCrew().getId())
                .isParent(comment.getParent()==null)    // true라면 부모댓글
                .isDeleted(comment.isSoftDeleted()==true) // true라면 삭제된 댓글
                .children(comment.getChildren() != null ? Comment.from(comment.getChildren()) : new LinkedList<>())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

