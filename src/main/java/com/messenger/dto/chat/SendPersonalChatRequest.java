package com.messenger.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Schema(description = "1:1 메시지 전송 RequestDTO")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SendPersonalChatRequest {

    @NotBlank
    @Schema(description = "수신 사용자 id", defaultValue = "oppositeUserId")
    private String receiverUserId;

    @NotBlank
    @Schema(description = "메시지 내용", defaultValue = "messageContent")
    private String content;
}
