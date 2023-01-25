package teamproject.pocoapoco.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import teamproject.pocoapoco.domain.dto.like.LikeResponse;
import teamproject.pocoapoco.domain.dto.response.Response;
import teamproject.pocoapoco.domain.entity.Like;
import teamproject.pocoapoco.service.LikeService;

@RestController
@RequestMapping("/api/v1/crews")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;
    @PostMapping("/{crewId}/like")
    public Response<LikeResponse> likeCrew(@PathVariable Long crewId, Authentication authentication){
        LikeResponse likeResponse = likeService.goodCrew(crewId,authentication.getName());
        return Response.success(likeResponse);
    }

    @GetMapping("/{crewId}/like")
    public Response<LikeResponse> getLike(@PathVariable Long crewId){
        LikeResponse likeResponse = likeService.getLike(crewId);
        return Response.success(likeResponse);
    }

}
