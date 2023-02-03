package teamproject.pocoapoco.controller.main.ui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import teamproject.pocoapoco.domain.dto.crew.CrewDetailResponse;
import teamproject.pocoapoco.domain.dto.crew.CrewRequest;
import teamproject.pocoapoco.domain.dto.crew.CrewResponse;
import teamproject.pocoapoco.domain.dto.crew.CrewStrictRequest;
import teamproject.pocoapoco.domain.dto.like.LikeViewResponse;
import teamproject.pocoapoco.domain.entity.Crew;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.repository.CrewRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.service.CrewService;
import teamproject.pocoapoco.service.ui.LikeViewService;

import javax.persistence.EntityNotFoundException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/view/v1/crews")
@Slf4j
@Api(tags = {"Crew Controller"})
public class CrewViewController {

    private final CrewService crewService;
    private final CrewRepository crewRepository;
    private final UserRepository userRepository;
    private final LikeViewService likeViewService;


    // 크루 게시글 작성
    @PostMapping("/view/v1/crews/")
    @ApiOperation(value = "크루 게시글 등록", notes = "")
    public String addCrew(@RequestBody CrewRequest crewRequest, Authentication authentication) {
        crewService.addCrew(crewRequest, authentication.getName());
        return "crew/write";
    }
    // 크루 게시물 상세 페이지
    @GetMapping("/view/v1/crews/{crewId}")
    public String detailCrew(@PathVariable Long crewId, Model model, Authentication authentication) {
        try {
            CrewDetailResponse details = crewService.detailCrew(crewId);
            int count = likeViewService.getLikeCrew(crewId);

            model.addAttribute("details", details);
            // 좋아요 개수 출력
            model.addAttribute("likeCnt",count);
        } catch (EntityNotFoundException e) {
            return "redirect:/index";
        }
        return "crew/read-crew";
    }

    // 크루 게시글 수정
    @PutMapping("/view/v1/crews/{crewId}")
    public String modifyCrew(@PathVariable Long crewId, @ModelAttribute CrewRequest crewRequest, Authentication authentication, Errors errors, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            return "crew/update-crew";
        }
        CrewResponse crewResponse = crewService.modifyCrew(crewId, crewRequest, authentication.getName());
        attributes.addFlashAttribute("message", "게시글을 수정했습니다.");

        return "redirect:" + "/view/v1/crews/update/" + crewResponse.getCrewId();
    }
    // 크루 게시글 수정화면
    @GetMapping("/view/v1/crews/update/{crewId}")
    public String updateCrew (@PathVariable Long crewId, Model model, Authentication authentication) {
        Crew crew  = crewRepository.findById(crewId).orElse(null);
        if(crew == null || !crew.getUser().getUsername().equals(authentication.getName())) {
            return "error/404";
        }
        CrewRequest crewRequest = new CrewRequest();
        crewRequest.setTitle(crew.getTitle());
        crewRequest.setContent(crew.getContent());

        model.addAttribute(crewRequest);
        model.addAttribute("crewId", crew.getId());

        return "crew/update-crew";
    }

    // 크루 게시글 삭제
    @DeleteMapping("/view/v1/crews/{crewId}")
    public String deleteCrew(@PathVariable Long crewId,Model model ,Authentication authentication) {
        Crew crew = crewRepository.findById(crewId).orElse(null);
        User user = userRepository.findById(crew.getUser().getId()).orElse(null);
        log.info("삭제 조회 중");

        if(crew == null || user == null) {
            return "error/404";
        }
        CrewResponse crewResponse = crewService.deleteCrew(crewId, authentication.getName());
        model.addAttribute("response",crewResponse);
        return "redirect:/";
    }

    // 좋아요 누르기
    @PostMapping("/view/v1/crews/{crewId}/like")
    public ResponseEntity likeCrew(@PathVariable Long crewId, Authentication authentication){
        LikeViewResponse likeViewResponse =  likeViewService.pressLike(crewId,authentication.getName());
        return new ResponseEntity<>(likeViewResponse, HttpStatus.OK);
    }



    // 크루 게시물 전체 조회
    @GetMapping()
    @ApiOperation(value = "크루 게시글 전체조회", notes = "")
    public String findAllCrew(Model model,
                                @PageableDefault(page = 0, size = 9, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CrewDetailResponse> list = crewService.findAllCrews(pageable);

        int nowPage = list.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 5, list.getTotalPages());
        int lastPage = list.getTotalPages();

        model.addAttribute("crewList", list);

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("lastPage", lastPage);

        return "main/main";
    }


    // 크루 게시물 지역 검색 조회
    @PostMapping("/strict")
    @ApiOperation(value = "크루 게시글 지역 검색조회", notes = "")
    public String findAllCrewWithStrict(String strict, Model model,
                                          @PageableDefault(page = 0, size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        CrewStrictRequest crewStrictRequest = new CrewStrictRequest(strict);
        System.out.println("test\n" + crewStrictRequest.getStrict());
        Page<CrewDetailResponse> list = crewService.findAllCrewsWithStrict(crewStrictRequest, pageable);

        int nowPage = list.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 5, list.getTotalPages());
        int lastPage = list.getTotalPages();

        model.addAttribute("crewList", list);

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("lastPage", lastPage);

        return "main/main";
    }
}
