package teamproject.pocoapoco.controller.main.ui.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import teamproject.pocoapoco.domain.dto.chat.ChatRoomDTO;
import teamproject.pocoapoco.domain.entity.Crew;
import teamproject.pocoapoco.domain.entity.User;
import teamproject.pocoapoco.domain.entity.part.Participation;
import teamproject.pocoapoco.exception.AppException;
import teamproject.pocoapoco.exception.ErrorCode;
import teamproject.pocoapoco.repository.CrewRepository;
import teamproject.pocoapoco.repository.UserRepository;
import teamproject.pocoapoco.repository.part.ParticipationRepository;
import teamproject.pocoapoco.service.chat.ChatRoomService;
import teamproject.pocoapoco.service.chat.ChatService;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/view/v1")
@Log4j2
public class RoomController {
    private final CrewRepository crewRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    //채팅방 목록 조회
    @GetMapping(value = "/rooms")
    public String rooms(Model model, Authentication authentication){
        log.info("# All Chat Rooms");
        model.addAttribute("list",chatRoomService.findByParticipation(authentication.getName()));
        return "chat/rooms";
    }

    //채팅방 개설
    @PostMapping(value = "/room")
    public String create(@RequestParam ChatRoomDTO chatRoomDTO, RedirectAttributes rttr, Authentication authentication){
        log.info("# Create Chat Room , name: " + chatRoomDTO.getName());
        rttr.addFlashAttribute("roomName", chatRoomService.createChatRoomDTO(chatRoomDTO, authentication.getName()));
        return "redirect:/view/v1/rooms";
    }

    //채팅방 조회
    @GetMapping("/room")
    public String getRoom(Long roomId, Long crewId, Model model){

        log.info("# get Chat Room, roomID : " + roomId);
        model.addAttribute("crewId",crewId);
        model.addAttribute("room", chatRoomService.findRoomById(roomId));
        return "chat/room";
    }


}
