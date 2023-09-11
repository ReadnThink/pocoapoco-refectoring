package teamproject.pocoapoco.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import teamproject.pocoapoco.domain.dto.Review.ReviewRequest;
import teamproject.pocoapoco.domain.dto.crew.review.CrewReviewDetailResponse;
import teamproject.pocoapoco.domain.dto.crew.review.CrewReviewResponse;
import teamproject.pocoapoco.domain.entity.Alarm;
import teamproject.pocoapoco.domain.entity.Crew;
import teamproject.pocoapoco.domain.entity.Review;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.enums.AlarmType;
import teamproject.pocoapoco.repository.AlarmRepository;
import teamproject.pocoapoco.repository.CrewRepository;
import teamproject.pocoapoco.repository.CrewReviewRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.util.SseSendStrategy;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
@Slf4j
public class CrewReviewService {
    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final CrewReviewRepository crewReviewRepository;
    private final AlarmRepository alarmRepository;
    private final SseSendStrategy sseSendStrategy;

    public CrewReviewService(final UserRepository userRepository,
                             final CrewRepository crewRepository,
                             final CrewReviewRepository crewReviewRepository,
                             final AlarmRepository alarmRepository,
                             @Qualifier("addReviewSseAlarm") final SseSendStrategy sseSendStrategy) {
        this.userRepository = userRepository;
        this.crewRepository = crewRepository;
        this.crewReviewRepository = crewReviewRepository;
        this.alarmRepository = alarmRepository;
        this.sseSendStrategy = sseSendStrategy;
    }

    // 리뷰 저장
    @Transactional
    public void addReview(ReviewRequest crewReviewRequest) {

        try{
            Crew crew = crewRepository.findById(crewReviewRequest.getCrewId().get(0)).get();
            User fromUser = userRepository.findById(crewReviewRequest.getFromUserId().get(0)).get();

            for (int i = 0; i < crewReviewRequest.getCrewId().size(); i++) {
                Review review = new Review();

                User toUser = userRepository.findById(crewReviewRequest.getToUserId().get(i)).get();

                review.of(crew, fromUser, toUser,
                        crewReviewRequest.getUserMannerScore().get(i), crewReviewRequest.getUserReview().get(i));
                crewReviewRepository.save(review);
                toUser.addReviewScore(review.getReviewScore());
                alarmRepository.save(Alarm.toEntityFromReview(toUser, fromUser, review, AlarmType.REVIEW_CREW, AlarmType.REVIEW_CREW.getText()));

                sendSseAlarm(toUser);
            }
        }catch (NullPointerException e){
            log.info("이용자 후기 NullPointerException : 작성 가능한 후기 내용이 없습니다.");
        }
    }

    private void sendSseAlarm(final User toUser) {
        var userSseKey = toUser.getUsername();
        if (sseSendStrategy.isUserLogin(userSseKey)) {
            sseSendStrategy.SendAlarm(
                    userSseKey,
                    "",
                    "",
                    "리뷰가 등록되었어요! 확인해 보세요!");
        }
    }

    @Transactional
    // 리뷰 작성 여부 확인
    public boolean findReviewedUser(Long crewId, User nowUser) {

        List<Review> reviewList = crewReviewRepository.findByCrewId(crewId);

        for(Review r : reviewList){
            if(r.getFromUser().getId() == nowUser.getId())
                return true;
        }
        return false;
    }

    @Transactional
    public Page<CrewReviewResponse> findAllReviewList(String userName, Pageable pageable) {
        User ToUser = userRepository.findByUserName(userName).get();
        Page<Review> allReviewList = crewReviewRepository.findByToUser(ToUser, pageable);
        return allReviewList.map(CrewReviewResponse::of);
    }

    // 리뷰 detail
    @Transactional
    public CrewReviewDetailResponse findReviewById(Long reviewId) {
        Review review = crewReviewRepository.findById(reviewId).get();



        return CrewReviewDetailResponse.builder()
                .id(reviewId)
                .fromUserName(review.getFromUser().getNickName())
                .crewTitle(review.getCrew().getTitle())
                .reviewContext(review.getReviewContext())
//                .reviews(reviews)
                .build();
    }

    public long getReviewAllCount(String userName) {
        User user = userRepository.findByUserName(userName).get();
        return crewReviewRepository.countReviewByToUser(user);
    }


    @Transactional
    public boolean isContainReview(Crew crew,User user){

        return crewReviewRepository.existsByCrewAndFromUser(crew,user);
    }


}
