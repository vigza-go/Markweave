package com.vigza.markweave.api.dto.Collaboration;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class InviteRequest {
   
    @NotNull
    private Long docId;
    
    @NotNull
    private Integer permission;


    @NotNull
    private Integer expTime;
}
