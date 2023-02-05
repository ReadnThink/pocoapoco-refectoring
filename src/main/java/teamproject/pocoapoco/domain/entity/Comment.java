package teamproject.pocoapoco.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import org.hibernate.annotations.Where;
import teamproject.pocoapoco.domain.dto.comment.CommentResponse;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Where(clause = "deleted_at is null")
public class Comment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id")
    Crew crew;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Setter
    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    private List<Comment> children = new LinkedList<>();

    public void setComment(String comment) {
        this.comment = comment;
    }

    public static List<CommentResponse> from(List<Comment> comments) {
        return comments.stream()
                .map(CommentResponse::of)
                .collect(Collectors.toList());
    }
}
