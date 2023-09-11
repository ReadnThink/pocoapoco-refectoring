package teamproject.pocoapoco.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import teamproject.pocoapoco.domain.dto.follow.FollowingResponse;
import teamproject.pocoapoco.domain.entity.Alarm;
import teamproject.pocoapoco.domain.entity.Follow;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.enums.AlarmType;
import teamproject.pocoapoco.exception.AppException;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.repository.AlarmRepository;
import teamproject.pocoapoco.repository.FollowRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.util.SseSendStrategy;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FollowService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final AlarmRepository alarmRepository;
    private final SseSendStrategy sseSendStrategy;

    public FollowService(final UserRepository userRepository,
                         final FollowRepository followRepository,
                         final AlarmRepository alarmRepository,
                         @Qualifier("followSseAlarm") final SseSendStrategy sseSendStrategy) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.alarmRepository = alarmRepository;
        this.sseSendStrategy = sseSendStrategy;
    }

    @Transactional
    public FollowingResponse follow(String followingUserId, Long userId){

        User followingUser = userRepository.findByUserName(followingUserId).orElseThrow(()->
        {
            throw new AppException(ErrorCode.USERID_NOT_FOUND,ErrorCode.USERID_NOT_FOUND.getMessage());
        });

        User user = userRepository.findById(userId).orElseThrow(()->
        {
            throw new AppException(ErrorCode.USERID_NOT_FOUND,ErrorCode.USERID_NOT_FOUND.getMessage());
        });
        //user 와 login한 user의 id가 같은경우
        if(userId.equals(followingUserId)){
            throw new AppException(ErrorCode.WRONG_PATH,ErrorCode.WRONG_PATH.getMessage());
        }

        Optional<Follow> follow = followRepository.findByFollowingUserIdAndFollowedUserId(followingUser.getId(), user.getId());


        if(followRepository.findByFollowingUserIdAndFollowedUserId(followingUser.getId(),user.getId()).isPresent()){
            followRepository.delete(follow.get());
            //팔로우 취소
            return new FollowingResponse(user.getUsername(),user.getNickName(),false, user.getImagePath());
        }else{
            //팔로우
            followRepository.save(new Follow(followingUser,user));
            //알림 저장
            alarmRepository.save(Alarm.toEntityFromFollow(user, followingUser, AlarmType.FOLLOW_CREW, AlarmType.FOLLOW_CREW.getText()));

            sendSseAlarm(followingUser, user);
        }
        return new FollowingResponse(user.getUsername(),user.getNickName(),true, user.getImagePath());

    }

    private void sendSseAlarm(final User followingUser, final User user) {
        var userSseKey = user.getUsername();
        if (sseSendStrategy.isUserLogin(userSseKey)) {
            sseSendStrategy.SendAlarm(
                    userSseKey,
                    followingUser.getNickName(),
                    "",
                    "님이 회원님을 팔로우 합니다💕");
        }
    }

    @Transactional
    public Integer followedCount(Long userId){ //해당 유저를 팔로우 하고 있는 유저의 수
        User user = userRepository.findById(userId).orElseThrow(()->
        {
            throw new AppException(ErrorCode.USERID_NOT_FOUND,ErrorCode.USERID_NOT_FOUND.getMessage());
        });
        return followRepository.countByFollowedUserId(userId);
    }
    @Transactional
    public Integer followingCount(Long userId){ //해당 유자가 팔로잉 하고 있는 유저의 수
        User user = userRepository.findById(userId).orElseThrow(()->
        {
            throw new AppException(ErrorCode.USERID_NOT_FOUND,ErrorCode.USERID_NOT_FOUND.getMessage());
        });
        return followRepository.countByFollowingUserId(userId);
    }


    @Transactional
    public Page<FollowingResponse> getFollowingList(Pageable pageable, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(()->
        {
            throw new AppException(ErrorCode.USERID_NOT_FOUND,ErrorCode.USERID_NOT_FOUND.getMessage());
        });
        Page<Follow> list = followRepository.findByFollowingUserId(pageable,userId);
        return list.map(FollowingResponse::followingResponse);
    }

    @Transactional
    public Page<FollowingResponse> getFollowedList(Pageable pageable, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(()->
        {
            throw new AppException(ErrorCode.USERID_NOT_FOUND,ErrorCode.USERID_NOT_FOUND.getMessage());
        });
        Page<Follow> list = followRepository.findByFollowedUserId(pageable,userId);
        return list.map(FollowingResponse::followedResponse);
    }

    @Transactional
    public void readAlarmsFollow(String username) {
        User user = userRepository.findByUserName(username).orElseThrow(() -> new AppException(ErrorCode.USERID_NOT_FOUND, ErrorCode.USERID_NOT_FOUND.getMessage()));
        List<Alarm> alarms = user.getAlarms();
        for (Alarm alarm : alarms) {
            boolean readOrNot = alarm.getReadOrNot();
            if (!readOrNot && alarm.getAlarmType().equals(AlarmType.FOLLOW_CREW)) {
                alarm.setReadOrNot();
            }
        }
    }
}
