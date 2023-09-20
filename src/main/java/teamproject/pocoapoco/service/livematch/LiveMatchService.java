package teamproject.pocoapoco.service.livematch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import teamproject.pocoapoco.domain.entity.Crew;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.domain.entity.chat.ChatRoom;
import teamproject.pocoapoco.domain.entity.part.Participation;
import teamproject.pocoapoco.exception.AppException;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.repository.CrewRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.repository.part.ParticipationRepository;
import teamproject.pocoapoco.service.sse.SseSender;
import teamproject.pocoapoco.service.sse.dto.AlarmTypeEnum;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

import static teamproject.pocoapoco.controller.main.api.sse.SseController.sseEmitters;
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
    private static String LIVE_MATCH = "_liveMatch";
    private static int CREW_LIMIT = 3;

    public LiveMatchService(final UserRepository userRepository,
                            final CrewRepository crewRepository,
                            final ParticipationRepository participationRepository,
                            final RedisTemplate<String, String> redisTemplate,
                            final SseSender sseSender) {
        this.userRepository = userRepository;
        this.crewRepository = crewRepository;
        this.participationRepository = participationRepository;
        this.redisTemplate = redisTemplate;
        this.sseSender = sseSender;
    }


    @Transactional
    public int randomMatch(String username, String sport) {
        log.info("==================실시간 매칭 Service 로직에 들어왔습니다 - username = {}, sport = {}", username, sport);

        redisTemplate
                .opsForZSet()
                .add(
                        sport,
                        username,
                        System.currentTimeMillis()
                );

        // logout시에 sport를 확인하기 위해 저장 -> 스케일 아웃이 되면 공유가 안되기에 Redis로 리펙토링 예정
        usersSports.put(username + LIVE_MATCH, sport);

        // 현재 대기열의 총 숫자 확인
        Long matchingUsersCnt = redisTemplate
                .opsForZSet()
                .zCard(sport);
        log.info("현재 redis 의 {} 종목 대기열의 숫자는 : {} 입니다", sport, matchingUsersCnt);

        //대기인원을 sport 종목에 따라 UI에 뿌려주는 로직
        sendSportListCntToUser(sport);
        // todo if문을 꼭 써야하나?
        if (matchingUsersCnt >= CREW_LIMIT) {
            log.info("==========================실시간 매칭 대기열이 3명이상이여서 crew 생성 진입");
            Set<String> usersToRandomMatch = redisTemplate
                    .opsForZSet()
                    .range(sport, 0, CREW_LIMIT);

            final String[] userNames = usersToRandomMatch
                    .stream()
                    .toArray(String[]::new);
            final User[] users = findUser(userNames);

            Crew savedLiveMatchCrew = makeCrew(users, sport);
            // participations 만들고 저장 후 crew에 저장
            makeParticipationsAndUpdateToCrew(users, savedLiveMatchCrew, sport);
            deleteRedisLiveMatchLists(userNames, sport);
            sendSseToUsers(users, savedLiveMatchCrew);

            // 대기리스트 확인
            log.info("삭제된 후 redis 대기열 : {}", redisTemplate.opsForZSet().zCard(sport));
        }
        return 1;
    }

    private void sendSseToUsers(User[] users, Crew savedLiveMatchCrew) {
        //todo SseSender 사용
        sendSse(users[0], savedLiveMatchCrew);
        sendSse(users[1], savedLiveMatchCrew);
        sendSse(users[2], savedLiveMatchCrew);
    }

    private void deleteRedisLiveMatchLists(String[] UserListInRedis, String sport) {
        // 랜덤매칭이 이루어진 3명을 대기리스트에서 삭제
        redisTemplate.opsForZSet().remove(sport, UserListInRedis[0]);
        redisTemplate.opsForZSet().remove(sport, UserListInRedis[1]);
        redisTemplate.opsForZSet().remove(sport, UserListInRedis[2]);

        //hashMap에서 user sports 삭제
        usersSports.remove(UserListInRedis[0] + LIVE_MATCH);
        usersSports.remove(UserListInRedis[1] + LIVE_MATCH);
        usersSports.remove(UserListInRedis[2] + LIVE_MATCH);
    }

    private void makeParticipationsAndUpdateToCrew(User[] users, Crew savedLiveMatchCrew, String sport) {
        // participations 만들기
        List<Participation> participationList = new ArrayList<>();
        Participation firstParticipation = makeParticipation(savedLiveMatchCrew, sport, users[0]);
        Participation secParticipation = makeParticipation(savedLiveMatchCrew, sport, users[1]);
        Participation thirdParticipation = makeParticipation(savedLiveMatchCrew, sport, users[2]);

        // participation 저장
        participationRepository.save(firstParticipation);
        participationRepository.save(secParticipation);
        participationRepository.save(thirdParticipation);

        // list에 저장
        participationList.add(firstParticipation);
        participationList.add(secParticipation);
        participationList.add(thirdParticipation);

        //저장된 크루에 participations 저장
        savedLiveMatchCrew.setParticipations(participationList);
    }

    private static Participation makeParticipation(final Crew savedLiveMatchCrew, final String sport, final User firstUser) {
        // todo Participation status 상태 Enum으로 가독성 높이기
        return Participation.builder()
                .status(2)
                .user(firstUser)
                .crew(savedLiveMatchCrew)
                .title(sport + "실시간 매칭🔥")
                .build();
    }


    private Crew makeCrew(User[] users, String sport) {
        // 방을 만들고 채팅방을 생성
        Crew crew = Crew.builder()
                .imagePath("67id36j0-디폴트.jpg")
                .strict("청진동 246 D1동 16층, 17층 ")
                .roadName("서울 종로구 종로3길 17 D1동 16층, 17층")
                .title(sport + "실시간 매칭🔥")
                .content(users[0].getUsername() + "님, "
                        + users[1].getUsername() + "님, "
                        + users[2].getUsername() + "님\n"
                        + "실시간 매칭이 성사되었습니다 \n" +
                        "채팅방에서 시간 장소를 조율해주세요")
                .crewLimit(CREW_LIMIT)
                .datepick(LocalDateTime.now().toString())
                .timepick(LocalDateTime.now().toString())
                .chatRoom(ChatRoom.builder()
                        .name(sport + "실시간 매칭")
                        .user(users[0])
                        .build()) //user에 참여자중 한명 넣으면 된다.. name = 타이틀이름
                .user(users[0])  // crew 만든사람
                .build();
        return crewRepository.save(crew);
    }

    private void sendSseToSportUser(List<String> user) {
        //sse 로직
        for (int i = 0; i < user.size(); i++) {
            if (sseEmitters.containsKey(user.get(i))) {
                log.info("실시간매칭 sport에 user들에게 대기인원 수 출력");
                SseEmitter sseEmitter = sseEmitters.get(user.get(i));
                try {
                    sseEmitter
                            .send(SseEmitter
                                    .event()
                                    .name(LIVE_MATCH_PARTICIPANT_CNT.getValue())
                                    .data(user.size()));
                } catch (Exception e) {
                    sseEmitters.remove(user.get(i));
                }
            }
        }
    }

    private void sendSse(User user, Crew savedLiveMatchCrew) {
        //sse 로직
        if (sseEmitters.containsKey(user.getUsername())) {
            log.info("실시간매칭 후 sse firstUser 작동");
            SseEmitter sseEmitter = sseEmitters.get(user.getUsername());
            try {
                sseEmitter
                        .send(SseEmitter
                                .event()
                                .name(AlarmTypeEnum.LIVE_MATCH.getValue())
                                .data(savedLiveMatchCrew.getChatRoom().getRoomId() + " " + savedLiveMatchCrew.getId()));
            } catch (Exception e) {
                sseEmitters.remove(savedLiveMatchCrew.getUser().getUsername());
            }
        }
    }

    public void sendSportListCntToUser(String sport) {
        List<String> users = new ArrayList<>();
        for (String user : usersSports.keySet()) {
            if (usersSports.get(user).equals(sport)) {
                String[] name = user.split(LIVE_MATCH);
                users.add(name[0]);
            }
        }
        log.info("sport 안에있는 users = {}", users);
        sendSseToSportUser(users);
    }

    @Transactional
    public int randomMatchCancel(@RequestParam String username) {
        String sport = usersSports.get(username + LIVE_MATCH);
        log.info("sport = {}", sport);
        // redis에 있는 userName을 삭제
        redisTemplate.opsForZSet().remove(sport, username);
        log.info("logout user = {}", username);
        usersSports.remove(username + LIVE_MATCH);
        //대기열 숫자 전송
        sendSportListCntToUser(sport);

        Long randomMatchListInRedisCnt = redisTemplate.opsForZSet().zCard(sport);
        log.info("현재 redis의 대기열의 숫자는 : {} 입니다", randomMatchListInRedisCnt);

        //sse 로직
        if (sseEmitters.containsKey(username)) {
            log.info("실시간 매칭 취소 -> 대기인원 정보 안보이게하기");
            SseEmitter sseEmitter = sseEmitters.get(username);
            try {
                sseEmitter.send(SseEmitter
                        .event()
                        .name(LIVE_MATCH_CANCEL.getValue())
                        .data(1));
            } catch (Exception e) {
                sseEmitters.remove(username);
            }
        }

        return 1;
    }

    private User[] findUser(String[] UserListInRedis) {
        User[] users = new User[CREW_LIMIT];
        for (int i = 0; i < CREW_LIMIT; i++) {
            users[i] = userRepository.findByUserName(UserListInRedis[i]).orElseThrow(() -> {
                return new AppException(ErrorCode.USERID_NOT_FOUND, ErrorCode.USERID_NOT_FOUND.getMessage());
            });
        }
        return users;
    }
}
