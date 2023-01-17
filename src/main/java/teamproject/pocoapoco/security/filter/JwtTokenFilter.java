package teamproject.pocoapoco.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import teamproject.pocoapoco.domain.dto.error.ErrorResponse;
import teamproject.pocoapoco.domain.dto.response.Response;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.security.provider.JwtProvider;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    /**
     * request 에서 전달받은 Jwt 토큰을 확인
     */

    private final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        request.setAttribute("existsToken", true); // 토큰 존재 여부 초기화
        if (isEmptyToken(token)) request.setAttribute("existsToken", false); // 토큰이 없는 경우 false로 변경

        if (token == null || !token.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        token = parseBearer(token);

        if (jwtProvider.validateToken(token)) {
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isEmptyToken(String token) {
        return token == null || "".equals(token);
    }

    private String parseBearer(String token) {
        return token.substring(BEARER.length());
    }

    /**
     * Security Chain 에서 발생하는 에러 응답 구성
     */
    public static void MakeError(HttpServletResponse response , ErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());
        ObjectMapper objectMapper = new ObjectMapper();

        ErrorResponse errorResponse = new ErrorResponse
                (errorCode, errorCode.getMessage());
        Response<ErrorResponse> resultResponse = Response.error(errorResponse);

        // 한글 출력을 위해 getWriter()
        response.getWriter().write(objectMapper.writeValueAsString(resultResponse));
    }
}
