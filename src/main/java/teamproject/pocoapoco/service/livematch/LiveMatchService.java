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
import java.util.*;
import java.util.stream.Collectors;

import static teamproject.pocoapoco.service.sse.dto.AlarmTypeEnum.*;

@Service
@Slf4j
public class LiveMatchService {
    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final ParticipationRepository participationRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private final SseSender sseSender;
    private HashMap<String, String> usersSports = new HashMap<>();
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

        // logout시에 sport를 확인하기 위해 저장 -> 스케일 아웃이 되면 공유가 안되기에 Redis로 리펙토링 예정
        // todo redis에 username이 아닌 User를 넣으면 삭제를 위한 Map이 필요 없지 않을까?
        usersSports.put(username + _LIVE_MATCH, sport);
        Long matchingUsersCnt = redisTemplate.opsForZSet().zCard(sport);
        log.info("현재 redis 의 {} 종목 대기열의 숫자는 : {} 입니다", sport, matchingUsersCnt);

        //대기인원을 sport 종목에 따라 UI에 뿌려주는 로직
        sendSportListCntToWaitingUsers(sport);

        if (matchingUsersCnt >= CREW_LIMIT) {
            log.info("==========================실시간 매칭 대기열이 3명이상이여서 crew 생성 진입");
            final List<User> users = findUser(redisTemplate.opsForZSet().range(sport, 0, CREW_LIMIT));

            Crew savedLiveMatchCrew = crewRepository.save(Crew.makeRandomMatchingCrew(users, sport, CREW_LIMIT));
            // participations 만들고 저장 후 crew에 저장
            makeParticipationAndUpdateToCrew(users, savedLiveMatchCrew, sport);
            deleteRedisLiveMatchLists(users, sport);
            sendSseToUsersAfterMatching(users, savedLiveMatchCrew);

            // 대기리스트 확인
            log.info("삭제된 후 redis 대기열 : {}", redisTemplate.opsForZSet().zCard(sport));
        }
        return 1;
    }

    private void deleteRedisLiveMatchLists(List<User> users, String sport) {
        users.stream()
                .map(User::getUsername)
                .forEach(userName -> {
                    redisTemplate.opsForZSet().remove(sport, userName);
                    usersSports.remove(userName + _LIVE_MATCH);
                });
    }

    private void makeParticipationAndUpdateToCrew(List<User> users, Crew savedLiveMatchCrew, String sport) {
        List<Participation> participationList = new ArrayList<>();
        users.stream().forEach(user
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
        String sport = usersSports.get(username + _LIVE_MATCH);
//        log.info("sport = {}", sport);
        // redis에 있는 userName을 삭제
        redisTemplate.opsForZSet().remove(sport, username);
//        log.info("logout user = {}", username);
        usersSports.remove(username + _LIVE_MATCH);
        //취소 후 대기열 숫자 전송
        sendSportListCntToWaitingUsers(sport);

        Long randomMatchListInRedisCnt = redisTemplate.opsForZSet().zCard(sport);
        log.info("현재 redis의 대기열의 숫자는 : {} 입니다", randomMatchListInRedisCnt);

        // 매칭 취소 후 매칭 중인 정보 cnt 줄이기
        sendSse(username, "", LIVE_MATCH_CANCEL);
        return 1;
    }

    private List<User> findUser(Set<String> userListInRedis) {
        // todo orElseThrow를 써야 안전한 코드가 될것 같다...
//        return userListInRedis.stream()
//                .map(userRepository::findByUserName)
//                .map(Optional::get)
//                .collect(Collectors.toList());

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

    private void sendSseToUsersAfterMatching(List<User> users, Crew savedLiveMatchCrew) {
        for (User user : users) {
            final String randomMatchChattingRoomURI = savedLiveMatchCrew.getChatRoom().getRoomId() + " " + savedLiveMatchCrew.getId();
            sendSse(user.getUsername(), randomMatchChattingRoomURI, LIVE_MATCH);
        }
    }

    public void sendSportListCntToWaitingUsers(String sport) {
        List<String> usersName = new ArrayList<>();
        for (String user : usersSports.keySet()) {
            if (usersSports.get(user).equals(sport)) {
                String[] name = user.split(_LIVE_MATCH);
                usersName.add(name[0]);
            }
        }
        log.info("sport 안에있는 usersName = {}", usersName);
        usersName.forEach(
                userName -> sendSse(userName, String.valueOf(usersName.size()), LIVE_MATCH_WAITING_CNT));
    }
}
