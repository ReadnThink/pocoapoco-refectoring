package teamproject.pocoapoco.service.livematch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import teamproject.pocoapoco.domain.entity.Crew;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.domain.entity.part.Participation;
import teamproject.pocoapoco.exception.AppException;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.repository.CrewRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.repository.part.ParticipationRepository;
import teamproject.pocoapoco.service.sse.SseSender;
import teamproject.pocoapoco.service.sse.UserSseKey;
import teamproject.pocoapoco.service.sse.dto.AlarmTypeEnum;
import teamproject.pocoapoco.service.sse.dto.SseAlarmData;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static teamproject.pocoapoco.service.sse.dto.AlarmTypeEnum.*;

@Service
@Slf4j
public class LiveMatchService {
    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final ParticipationRepository participationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SseSender sseSender;
    private static String _LIVE_MATCH = "_liveMatch";
    private static int CREW_LIMIT = 3;

    public LiveMatchService(final UserRepository userRepository, final CrewRepository crewRepository, final ParticipationRepository participationRepository, final RedisTemplate<String, String> redisTemplate, final SseSender sseSender) {
        this.userRepository = userRepository;
        this.crewRepository = crewRepository;
        this.participationRepository = participationRepository;
        this.redisTemplate = redisTemplate;
        this.sseSender = sseSender;
    }

    @Transactional
    public int randomMatch(String username, String sport) {
        log.info("==================실시간 매칭 Service 로직에 들어왔습니다 - username = {}, sport = {}", username, sport);
        redisTemplate.opsForZSet().add(sport, username, System.currentTimeMillis());
        redisTemplate.opsForSet().add(username + _LIVE_MATCH, sport);
        sendSportListCntToWaitingUsers(sport);

        Long matchingUsersCnt = redisTemplate.opsForZSet().zCard(sport);
        if (matchingUsersCnt >= CREW_LIMIT) {
            log.info("==========================실시간 매칭 대기열이 3명이상이여서 crew 생성 진입");
            final List<User> users = findUser(redisTemplate.opsForZSet().range(sport, 0, CREW_LIMIT));
            // todo 메서드의 매개변수가 중복되는것을 객체화 할 수는 없을까?
            Crew savedLiveMatchCrew = crewRepository.save(Crew.makeRandomMatchingCrew(users, sport, CREW_LIMIT));
            makeParticipationAndUpdateToCrew(users, savedLiveMatchCrew, sport);
            deleteRedisLiveMatchLists(users, sport);
            sendSseToUsersAfterMatchingAndRedirectToChattingRoom(users, savedLiveMatchCrew);
            log.info("삭제된 후 redis 대기열 : {}", redisTemplate.opsForZSet().zCard(sport));
        }
        return 1;
    }

    private void deleteRedisLiveMatchLists(List<User> users, String sport) {
        users.stream()
                .map(User::getUsername)
                .forEach(userName -> {
                    redisTemplate.opsForZSet().remove(sport, userName);
                });
    }

    private void makeParticipationAndUpdateToCrew(List<User> users, Crew savedLiveMatchCrew, String sport) {
        List<Participation> participationList = new ArrayList<>();
        users.forEach(user
                -> participationList.add(makeParticipation(savedLiveMatchCrew, sport, user)
        ));
        participationRepository.saveAll(participationList);
        savedLiveMatchCrew.setParticipations(participationList);
    }

    private Participation makeParticipation(final Crew savedLiveMatchCrew, final String sport, final User firstUser) {
        return Participation.builder()
                .status(2) // todo Participation status 상태 Enum으로 가독성 높이기
                .user(firstUser)
                .crew(savedLiveMatchCrew)
                .title(sport + "실시간 매칭🔥")
                .build();
    }

    @Transactional
    public int randomMatchCancel(@RequestParam String username) {
        final String sport = redisTemplate.opsForSet().pop(username + _LIVE_MATCH);
        redisTemplate.opsForZSet().remove(sport, username);

        sendSportListCntToWaitingUsers(sport);
        sendSse(username, "", LIVE_MATCH_CANCEL);
        return 1;
    }

    private List<User> findUser(Set<String> userListInRedis) {
        List<User> users = new ArrayList<>();
        for (String userName : userListInRedis) {
            users.add(userRepository.findByUserName(userName).orElseThrow(
                    () -> new AppException(ErrorCode.USERID_NOT_FOUND, ErrorCode.USERID_NOT_FOUND.getMessage())
            ));
        }
        return users;
    }

    private void sendSse(String userName, String data, AlarmTypeEnum alarmTypeEnum) {
        sseSender.sendAlarmRandomMatch(
                UserSseKey.builder()
                        .userSseKey(userName)
                        .build(),
                SseAlarmData.builder()
                        .data(data)
                        .alarmTypeEnum(alarmTypeEnum)
                        .build()
        );
    }

    private void sendSseToUsersAfterMatchingAndRedirectToChattingRoom(List<User> users, Crew savedLiveMatchCrew) {
        for (User user : users) {
            final String randomMatchChattingRoomURI = savedLiveMatchCrew.getChatRoom().getRoomId() + " " + savedLiveMatchCrew.getId();
            sendSse(user.getUsername(), randomMatchChattingRoomURI, LIVE_MATCH);
        }
    }

    public void sendSportListCntToWaitingUsers(String sport) {
        List<String> usersName = new ArrayList<>(redisTemplate.opsForZSet().range(sport, 0, CREW_LIMIT));
        usersName.forEach(
                userName -> sendSse(userName, String.valueOf(usersName.size()), LIVE_MATCH_WAITING_CNT));
    }
}
