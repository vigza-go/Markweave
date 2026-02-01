package com.vigza.markweave.api.dto;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class InviteRequest {
   
    @NotNull
    private Long docId;
    
    @NotNull
    private Long targetUserId;

    @NotNull
    private Integer permission;


}
