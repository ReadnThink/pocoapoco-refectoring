package teamproject.pocoapoco.domain.dto.like;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LikeViewResponse {
    private int likeCheck;
    private int count;
    private String userName;
}
