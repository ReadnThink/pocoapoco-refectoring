package teamproject.pocoapoco.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import teamproject.pocoapoco.domain.dto.user.*;
import teamproject.pocoapoco.domain.entity.Sport;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.enums.UserRole;
import teamproject.pocoapoco.exception.AppException;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.fixture.UserEntityFixture;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.security.config.EncrypterConfig;
import teamproject.pocoapoco.security.provider.JwtProvider;
import teamproject.pocoapoco.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {


    @Nested
    @DisplayName("회원가입 테스트")
    public class JoinTest{

        @Mock
        UserRepository userRepository;

        @Mock
        EncrypterConfig config;

        @Mock
        JwtProvider jwtProvider;

        UserJoinRequest userJoinRequest1 = UserJoinRequest.builder()
                .userId("아이디")
                .userName("닉네임")
                .password("비밀번호")
                .passwordConfirm("비밀번호")
                .address("서울시 강남구")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(false)
                .build();
        UserJoinRequest userJoinRequest2 = UserJoinRequest.builder()
                .userId("아이디")
                .userName("닉네임312")
                .password("비밀번호")
                .passwordConfirm("비밀번호")
                .address("서울시 강남구")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(false)
                .build();

        UserJoinRequest userJoinRequest3 = UserJoinRequest.builder()
                .userId("아이디12")
                .userName("닉네임")
                .password("비밀번호")
                .passwordConfirm("비밀번호")
                .address("서울시 강남구")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(false)
                .build();

        UserJoinRequest userJoinRequest4 = UserJoinRequest.builder()
                .userId("아이디12")
                .userName("닉네임")
                .password("비밀번호")
                .passwordConfirm("비밀번호wex")
                .address("서울시 강남구")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(false)
                .build();

        User user1 = UserEntityFixture.get(userJoinRequest1);

        UserService userService;

        private UserLoginResponse userLoginResponse;

        // encoder 설정
        @BeforeEach
        public void 세팅(){

            when(config.encoder()).thenReturn(new BCryptPasswordEncoder());
            userService = new UserService(userRepository, config,new RedisTemplate<>(),jwtProvider);


        }


        @Test
        @DisplayName("회원가입 성공")
        public void 회원가입테스트1() {

            //given
            UserJoinResponse userJoinResponse = UserJoinResponse.builder()
                    .userId("아이디")
                    .message("회원가입 되었습니다.").build();


            // when
            when(userRepository.save(any())).thenReturn(user1);
            UserJoinResponse result = userService.addUser(userJoinRequest1);

            // then
            assertAll(
                    () -> assertEquals(userJoinResponse.getUserId(), result.getUserId()),
                    () -> assertEquals(userJoinResponse.getMessage(), result.getMessage()));
        }



        @Test
        @DisplayName("회원가입 실패1 - 아이디 중복")
        public void 회원가입테스트2(){
            // given
            userRepository.save(user1);


            //when
            when(userRepository.save(any())).thenThrow(new AppException(ErrorCode.DUPLICATED_USERID, ErrorCode.DUPLICATED_USERID.getMessage()));
            RuntimeException exception = Assertions.assertThrows(AppException.class,
                    () -> userService.addUser(userJoinRequest2));

            //then
            assertEquals(exception.getMessage(), "이미 존재하는 아이디 입니다.");

        }

        @Test
        @DisplayName("회원가입 실패2 - 닉네임 중복")
        public void 회원가입테스트3() {


            //given
            userRepository.save(user1);


            // when
            when(userRepository.save(any())).thenThrow(new AppException(ErrorCode.DUPLICATED_USERNAME, ErrorCode.DUPLICATED_USERNAME.getMessage()));
            RuntimeException exception = Assertions.assertThrows(AppException.class,
                    () -> userService.addUser(userJoinRequest3));

            //then
            assertEquals(exception.getMessage(), "이미 존재하는 닉네임 입니다.");
        }

        @Test
        @DisplayName("회원가입 실패3 - 확인용 비밀번호가 비밀번호와 일치하지 않음")
        public void 회원가입테스트4() {

            //given: userJoinRequest4


            //when
            lenient().when(userRepository.save(any())).thenThrow(new AppException(ErrorCode.DUPLICATED_USERNAME, ErrorCode.DUPLICATED_USERNAME.getMessage()));
            RuntimeException exception = Assertions.assertThrows(AppException.class,
                    () -> userService.addUser(userJoinRequest4));

            //then
            assertEquals(exception.getMessage(), "패스워드가 잘못되었습니다.");
        }

    }

    @Nested
    @DisplayName("로그인 Test")
    class Login {
        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private EncrypterConfig config;

        @Mock
        private JwtProvider jwtProvider;

        @InjectMocks
        UserService userService;

        @Value("${jwt.token.secret}") String secretKey;

        private UserLoginRequest userLoginRequest;
        private UserLoginResponse userLoginResponse;

        User user = User.builder()
                .id(1L)
                .userId("userId1234")
                .userName("닉네임")
                .password("pass1234")
                .address("서울시 강남구")
                .sport(Sport.setSport(true, false, false))
                .manner(1)
                .role(UserRole.ROLE_USER)
                .build();
        // join request
        UserJoinRequest userJoinRequest1 =  UserJoinRequest.builder()
                .userId("userId1234")
                .userName("닉네임")
                .password("pass1234")
                .passwordConfirm("pass1234")
                .address("서울시 강남구")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(false)
                .build();

        private UserJoinRequest createJoinRequest() {
            return UserJoinRequest.builder()
                    .userId("userId1234")
                    .userName("닉네임")
                    .password("pass1234")
                    .passwordConfirm("pass1234")
                    .address("서울시 강남구")
                    .likeSoccer(true)
                    .likeJogging(false)
                    .likeTennis(false)
                    .build();
        }
        @Mock
        UserService userService;
        @Mock
        RedisTemplate<String,String> redisTemplate;


        // encoder 설정
        @BeforeEach
        public void 세팅(){
            when(config.encoder()).thenReturn(new BCryptPasswordEncoder());
            userService = new UserService(userRepository, config,new RedisTemplate<>(),jwtProvider);
            userLoginRequest = new UserLoginRequest("userId1234", "pass1234");
            user = User.toEntity(userJoinRequest1.getUserId(), userJoinRequest1.getUserName(), userJoinRequest1.getAddress(),
                    config.encoder().encode(userJoinRequest1.getPassword()), userJoinRequest1.getLikeSoccer(),
                    userJoinRequest1.getLikeJogging(), userJoinRequest1.getLikeTennis());

        }
        @Test
        @DisplayName("로그인 성공")
        public void 로그인테스트1() {
            // given
            UserJoinRequest addUser = createJoinRequest();
            when(userRepository.save(any())).thenReturn(user);
            UserJoinResponse userJoinResponse = userService.addUser(addUser);

            when(userRepository.findByUserId(userJoinResponse.getUserId())).thenReturn(Optional.of(user));
            when(jwtProvider.generateAccessToken(user)).thenReturn("accessToken");
            when(jwtProvider.generateRefreshToken(user)).thenReturn("refreshToken");

            //when
            UserLoginResponse response = userService.login(userLoginRequest);

            // then
            assertAll(
                    () -> assertEquals(userLoginResponse.getRefreshToken(), response.getRefreshToken()),
                    () -> assertEquals(userLoginResponse.getAccessToken(), response.getAccessToken()));
        }

        @Test
        @DisplayName("토큰 재발행")
        void regenerateToken() {
            // given
            UserJoinRequest addUser = createJoinRequest();
            when(userRepository.save(any())).thenReturn(user);
            UserJoinResponse userJoinResponse = userService.addUser(addUser);

            // when
            UserLoginResponse response = userService.login(userLoginRequest);
            String prevAccessToken = response.getAccessToken();
            String prevRefreshToken = response.getRefreshToken();

            ReIssueRequest regenerateToken = new ReIssueRequest(
                    response.getRefreshToken(), prevAccessToken
            );

            ReIssueResponse regeneratedToken = userService.regenerateToken(regenerateToken);

            // then
            assertThat(regeneratedToken.getAccessToken()).isNotEqualTo(prevRefreshToken);
            assertThat(regeneratedToken.getAccessToken()).isNotEqualTo(prevAccessToken);
        }

    }

    @Nested
    @DisplayName("프로필 수정 테스트")
    public class ReviseProfile{

        @Mock
        UserRepository userRepository;

        @Mock
        EncrypterConfig config;

        @Mock
        JwtProvider jwtProvider;

        UserProfileRequest userProfileRequest1 = UserProfileRequest.builder()
                .userName("닉네임")
                .address("주소")
                .password("비밀번호")
                .passwordConfirm("비밀번호")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(true)
                .build();

        UserProfileRequest userProfileRequest2 = UserProfileRequest.builder()
                .userName("닉네임")
                .address("주소")
                .password("비밀번호")
                .passwordConfirm("비밀번호123")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(true)
                .build();

        UserProfileResponse userProfileResponse1 = UserProfileResponse.builder()
                .userName("닉네임")
                .address("주소")
                .likeSoccer(true)
                .likeJogging(false)
                .likeTennis(true)
                .build();

        UserService userService;

        User user1 = User.builder()
                .userId("아이디")
                .userName("닉네임")
                .address("주소")
                .sport(Sport.setSport(true, false, true))
                .build();

        String token = "Bearer token.token.token";


        // encoder 설정
        @BeforeEach
        public void 세팅(){

            lenient().when(config.encoder()).thenReturn(new BCryptPasswordEncoder());
            userService = new UserService(userRepository, config);

            lenient().when(jwtProvider.validateToken(any())).thenReturn(true);


        }


        @Test
        @DisplayName("프로필 수정 성공")
        public void 프로필수정테스트1() {

            //given


            // when
            lenient().when(userRepository.save(any())).thenReturn(user1);
            UserProfileResponse result = userService.modifyMyUserInfo(token, userProfileRequest1);

            // then
            assertAll(
                    () -> assertEquals(userProfileResponse1.getUserName(), result.getUserName()),
                    () -> assertEquals(userProfileResponse1.getAddress(), result.getAddress()));
        }



        @Test
        @DisplayName("프로필 수정 실패1 - 비밀번호 확인 실패")
        public void 프로필수정테스트2(){

            // given: 전역 변수


            //when
            lenient().when(userRepository.save(any())).thenThrow(new AppException(ErrorCode.INVALID_PASSWORD, ErrorCode.INVALID_PASSWORD.getMessage()));

            AppException exception = Assertions.assertThrows(AppException.class,
                    () -> userService.modifyMyUserInfo(token, userProfileRequest2));

            //then
            assertEquals(exception.getMessage(), "패스워드가 일치하지 않습니다.");

        }

        @Test
        @DisplayName("프로필 수정 실패2 - 유효하지 않은 토큰")
        public void 프로필수정테스트3() {

            //given


            // when
            lenient().when(userRepository.save(any())).thenThrow(new AppException(ErrorCode.INVALID_TOKEN, ErrorCode.INVALID_TOKEN.getMessage()));
            AppException exception = Assertions.assertThrows(AppException.class,
                    () -> userService.modifyMyUserInfo(token, userProfileRequest1));

            //then
            assertEquals(exception.getMessage(), "잘못된 토큰입니다.");
        }

        @Test
        @DisplayName("프로필 수정 실패3 - 사용자를 찾지 못함")
        public void 프로필수정테스트4() {

            //given: 전역변수


            //when
            lenient().when(userRepository.save(any())).thenThrow(new AppException(ErrorCode.USERID_NOT_FOUND, ErrorCode.USERID_NOT_FOUND.getMessage()));
            AppException exception = Assertions.assertThrows(AppException.class,
                    () -> userService.modifyMyUserInfo(token, userProfileRequest1));

            //then
            assertEquals(exception.getMessage(), "아이디가 존재하지 않습니다.");
        }

    }


}