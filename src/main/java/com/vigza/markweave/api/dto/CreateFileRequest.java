package com.vigza.markweave.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateFileRequest {
   @NotBlank(message = "文件(夹)名称不能为空")
   private String name;
   
   @NotNull(message = "父级id不能为空")
   private Long parentId;
  
   private Boolean isFolder = false;
   
}
