package com.vigza.markweave.api.dto;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class MoveRequest {
    @NotNull
    private Long id;

    @NotNull
    private Long targetFolderId;
}
