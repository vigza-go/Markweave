package com.vigza.markweave.api.dto.Collaboration;

import lombok.Data;

@Data
public class CollaboratorVo {
   private Long userId;
   private String nickName;
   private String headUrl;
   private Integer permission; 
}
