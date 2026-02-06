package com.vigza.markweave.api.dto.FileSystem;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CreateShortcutRequest {
    @NotNull
    private Long faId;
    @NotNull
    private Long srcNodeId;
}
