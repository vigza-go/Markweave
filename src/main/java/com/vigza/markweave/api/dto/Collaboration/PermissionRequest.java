package com.vigza.markweave.api.dto.Collaboration;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class PermissionRequest {
    @NotNull
    private Long docId;
    @NotNull
    private Long targetUserId;
    @NotNull
    private Integer permission;
}
