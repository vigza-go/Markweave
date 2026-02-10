package com.vigza.markweave.api.dto.Collaboration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class InviteRequest {
   
    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long docId;

    @NotNull
    private String fileName;

    @NotNull
    private Integer permission;

    @NotNull
    private Integer expTime;
}
