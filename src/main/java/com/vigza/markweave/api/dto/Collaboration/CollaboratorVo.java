package com.vigza.markweave.api.dto.Collaboration;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class CollaboratorVo {
   @JsonSerialize(using = ToStringSerializer.class)
   private Long userId;
   private String nickName;
   private String headUrl;
   private Integer permission; 
}
