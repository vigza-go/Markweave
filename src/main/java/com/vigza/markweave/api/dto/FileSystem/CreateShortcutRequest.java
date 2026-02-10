package com.vigza.markweave.api.dto.FileSystem;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class CreateShortcutRequest {
    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long faId;
    @NotNull
    private Long srcNodeId;
}
