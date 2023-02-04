package teamproject.pocoapoco.domain.dto.user;

import lombok.Builder;
import lombok.Getter;
import teamproject.pocoapoco.domain.entity.User;

@Builder
@Getter
public class UserProfileResponse {

    private String userId;
    private String userName;
    private String address;
    private Boolean likeSoccer;
    private Boolean likeJogging;
    private Boolean likeTennis;

    public static UserProfileResponse fromEntity(User user){

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .address(user.getAddress())
                .likeSoccer(user.getSport().isSoccer())
                .likeJogging(user.getSport().isJogging())
                .likeTennis(user.getSport().isTennis())
                .build();
    }
}
