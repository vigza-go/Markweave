package com.vigza.markweave.api.dto.FileSystem;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class MoveRequest {
    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long nodeId;

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetFolderId;
}
