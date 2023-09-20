package teamproject.pocoapoco.controller.main.api.match;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import teamproject.pocoapoco.service.livematch.LiveMatchService;

import javax.transaction.Transactional;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Slf4j
public class RandomMatchController {
    private final LiveMatchService liveMatchService;

    @PostMapping("/live")
    @Transactional
    public int randomMatch(@RequestParam String username, @RequestParam String sport) {
        return liveMatchService.randomMatch(username, sport);
    }

    @PostMapping("/live/cancel")
    @Transactional
    public int randomMatchCancel(@RequestParam String username) {
        return liveMatchService.randomMatchCancel(username);
    }

}
