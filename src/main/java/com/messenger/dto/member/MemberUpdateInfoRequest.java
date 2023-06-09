package com.messenger.dto.member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "회원 정보 변경 RequestDTO")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberUpdateInfoRequest {

    @Schema(description = "비밀번호", defaultValue = "password")
    private String password;

    @Schema(description = "이름", defaultValue = "memberName")
    private String name;
    
    @Schema(description = "상태 메시지", defaultValue = "memberStatusMessage")
    private String statusMessage;
}
