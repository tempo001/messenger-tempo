package com.messenger.web;

import com.messenger.domain.Chat;
import com.messenger.domain.PaginationWrapper;
import com.messenger.service.ChatService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 1:1 메시지를 전송
     * @param receiverUserId 수신 사용자 id
     * @param content    메시지 내용
     * @param session    세션
     * @return 메시지 객체
     */
    @PostMapping("/api/v1/chat")
    public ResponseEntity<Chat> sendPersonalChat(
                @RequestParam String receiverUserId,
                @RequestParam String content,
                @NonNull HttpSession session) {
        String sessionUserId = getSessionUserId(session);
        if (sessionUserId == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        Chat ret;
        try {
            ret = chatService.sendPersonalChat(
                    Chat.builder()
                            .senderUserId(sessionUserId)
                            .receiverUserId(receiverUserId)
                            .content(content)
                            .build());
        } catch (DuplicateKeyException e) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        } catch (NullPointerException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    /**
     * 자신이 전송한 1:1 메시지 하나를 삭제
     * @param chatId 메시지 id
     * @param session 세션
     * @return "success"
     */
    @DeleteMapping("/api/v1/chat/{chatId}")
    public ResponseEntity<String> deletePersonalChat(
                @PathVariable long chatId,
                HttpSession session) {
        String sessionUserId = getSessionUserId(session);
        if (sessionUserId == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        try {
            chatService.deletePersonalChat(chatId, sessionUserId);
        } catch (NullPointerException e) {
            log.debug(e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("success", HttpStatus.OK);
    }

    /**
     * (개발자용) 전체 1:1 메시지 목록
     * 삭제된 메시지도 포함된다
     * @param prevId 이전 조회한 마지막 메시지 id
     * @param size 조회할 메시지 개수
     * @return 메시지 객체 리스트
     */
    @GetMapping("/api/v1/chat")
    public ResponseEntity<PaginationWrapper> listAllPersonalChat(
                @RequestParam(required = false) Integer prevId,
                @RequestParam(required = false, defaultValue = "3") Integer size) {
        List<Chat> list = chatService.listAllPersonalChat(prevId, size);
        return new ResponseEntity<>(new PaginationWrapper(list), HttpStatus.OK);
    }

    /**
     * 자신이 전송한 모든 1:1 메시지의 목록
     * @param prevId 이전 조회한 마지막 메시지 id
     * @param size 조회할 메시지 개수
     * @param session 세션
     * @return 메시지 객체 리스트
     */
    @GetMapping("/api/v1/chat/sent")
    public ResponseEntity<PaginationWrapper> listSentPersonalChat(
                @RequestParam(required = false) Integer prevId,
                @RequestParam(required = false, defaultValue = "3") Integer size,
                HttpSession session) {
        String sessionUserId = getSessionUserId(session);
        if (sessionUserId == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        List<Chat> list = chatService.listPersonalChatBySender(sessionUserId, prevId, size);
        return new ResponseEntity<>(new PaginationWrapper(list), HttpStatus.OK);
    }

    /**
     * 자신이 수신한 모든 1:1 메시지 목록
     * @param prevId 이전 조회한 마지막 메시지 id
     * @param size 조회할 메시지 개수
     * @param session 세션
     * @return 메시지 객체 리스트
     */
    @GetMapping("/api/v1/chat/received")
    public ResponseEntity<PaginationWrapper> listReceivedPersonalChat(
                @RequestParam(required = false) Integer prevId,
                @RequestParam(required = false, defaultValue = "3") Integer size,
                HttpSession session) {
        String sessionUserId = getSessionUserId(session);
        if (sessionUserId == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        List<Chat> list = chatService.listPersonalChatByReceiver(sessionUserId, prevId, size);
        return new ResponseEntity<>(new PaginationWrapper(list), HttpStatus.OK);
    }

    /**
     * 1:1 채팅 그룹에 입장
     * 해당 그룹의 메시지 목록을 최신순으로 가져오고, 가장 최근 수신한 메시지를 읽음 표시한다
     * @param oppositeUserId 상대방 사용자 id
     * @param size 조회할 메시지 개수
     * @param session 세션
     * @return 메시지 객체 리스트, 가장 최근 수신한 메시지
     */
    @GetMapping("/api/v1/chat/personal_chat/{oppositeUserId}/enter")
    public ResponseEntity<PaginationWrapper> enterPersonalChatGroup(
                @PathVariable String oppositeUserId,
                @RequestParam(required = false, defaultValue = "3") Integer size,
                HttpSession session) {
        String sessionUserId = getSessionUserId(session);
        if (sessionUserId == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        // 해당 그룹의 메시지 목록을 가져옴
        List<Chat> chatList = listPersonalChatByGroup(sessionUserId, oppositeUserId, null, size);
        PaginationWrapper result = new PaginationWrapper(chatList);

        // 가장 최근 수신한 메시지를 읽음 표시
        Optional<Chat> markedChat = chatService.markPersonalChatAsReadByGroup(sessionUserId, oppositeUserId);

        result.put("latest received chat", markedChat.orElse(null));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 특정 1:1 채팅 그룹의 메시지 목록 (최신순)
     * 자신과 상대방의 사용자 id를 기준으로 검색한다
     * @param oppositeUserId 상대방 사용자 id
     * @param prevId 이전 조회한 마지막 메시지 id
     * @param size 조회할 메시지 개수
     * @param session 세션
     * @return 메시지 객체 리스트
     */
    @GetMapping("/api/v1/chat/personal_chat/{oppositeUserId}")
    public ResponseEntity<PaginationWrapper> listPersonalChatByGroup(
                @PathVariable String oppositeUserId,
                @RequestParam(required = false) Integer prevId,
                @RequestParam(required = false, defaultValue = "3") Integer size,
                HttpSession session) {
        String sessionUserId = getSessionUserId(session);
        if (sessionUserId == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        List<Chat> chatList = listPersonalChatByGroup(sessionUserId, oppositeUserId, prevId, size);
        return new ResponseEntity<>(new PaginationWrapper(chatList), HttpStatus.OK);
    }

    @Nullable
    private static String getSessionUserId(HttpSession session) {
        return (String) session.getAttribute(MemberController.SESSION_KEY_USER_ID);
    }

    private List<Chat> listPersonalChatByGroup(
            String userId,
            String oppositeUserId,
            Integer prevId,
            Integer size) {
        return chatService.listPersonalChatByGroup(userId, oppositeUserId, prevId, size);
    }
}
