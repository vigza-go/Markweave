package com.vigza.markweave.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RenameRequest {
    @NotNull
    private Long id;

    @NotBlank
    private String newName;
}
