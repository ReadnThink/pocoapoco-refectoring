package teamproject.pocoapoco.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import teamproject.pocoapoco.domain.dto.crew.CrewRequest;
import teamproject.pocoapoco.domain.entity.chat.ChatRoom;
import teamproject.pocoapoco.domain.entity.part.Participation;
import teamproject.pocoapoco.enums.SportEnum;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
//@Where(clause = "deleted_at is null")
public class Crew extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String strict;
    private String roadName;
    private String title;
    private String content;
    private Integer crewLimit;

    private String imagePath;

    private String datepick;
    private String timepick;
    private Integer finish;


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="room_id")
    private ChatRoom chatRoom;

    @Enumerated(value = EnumType.STRING)
    private SportEnum sportEnum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "crew")
    private List<Like> likes = new ArrayList<>();

    //참여중인사람 조회
    @OneToMany(mappedBy = "crew")
    private List<Participation> participations = new ArrayList<>();


    public void setFinish(Integer finish){
        this.finish = finish;
    }
    public void setChatRoom(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }

    public void setParticipations(List<Participation> participations){
        this.participations = participations;
    }

    public void of(CrewRequest request) {
        this.strict = request.getStrict();
        this.title = request.getTitle();
        this.content = request.getContent();
        this.crewLimit = request.getCrewLimit();
    }

    public static Crew makeRandomMatchingCrew(List<User> users, String sport, int crewLimit){
        return Crew.builder()
                .imagePath("67id36j0-디폴트.jpg")
                .strict("청진동 246 D1동 16층, 17층 ")
                .roadName("서울 종로구 종로3길 17 D1동 16층, 17층")
                .title(sport + "실시간 매칭🔥")
                .content(users.stream()
                        .map(User::getUsername)
                        .collect(Collectors.joining("님, ", "", "님\n"))
                        + "실시간 매칭이 성사되었습니다 \n" +
                        "채팅방에서 시간 장소를 조율해주세요")
                .crewLimit(crewLimit)
                .datepick(LocalDateTime.now().toString())
                .timepick(LocalDateTime.now().toString())
                .chatRoom(ChatRoom.builder()
                        .name(sport + "실시간 매칭")
                        .user(users.get(0)) //user에 참여자중 한명 넣으면 된다.. name = 타이틀이름
                        .build())
                .user(users.get(0))  // crew 만들사람
                .build();
    }
}
