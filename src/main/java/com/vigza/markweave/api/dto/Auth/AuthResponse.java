package com.vigza.markweave.api.dto.Auth;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserDTO user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String account;
        private String nickname;
        private String headUrl;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userSpaceNodeId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userShareSpaceNodeId;
        private long type;
    }
}