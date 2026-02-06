package com.vigza.markweave.api.dto.FileSystem;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class MoveRequest {
    @NotNull
    private Long nodeId;

    @NotNull
    private Long targetFolderId;
}
