package teamproject.pocoapoco.domain.dto.chat;


import lombok.*;
import org.springframework.web.socket.WebSocketSession;
import teamproject.pocoapoco.domain.entity.chat.ChatRoom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ChatRoomDTO {

    private Long roomId;
    private String name;
    private Set<WebSocketSession> sessions = new HashSet<>();
    //WebSocketSession은 Spring에서 Websocket Connection이 맺어진 세션

    public static List<ChatRoomDTO> createList(List<ChatRoom> list){
        List<ChatRoomDTO> result = new ArrayList<>();
        for(ChatRoom chatRoom : list){
            result.add(ChatRoomDTO.builder().roomId(chatRoom.getRoomId())
                    .name(chatRoom.getName())
                    .build());
        }
        return result;
    }
}