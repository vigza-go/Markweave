package com.vigza.markweave.api.dto.FileSystem;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class CreateFileRequest {
   @NotBlank(message = "文件(夹)名称不能为空")
   private String fileName;

   @NotNull(message = "父级id不能为空")
   @JsonSerialize(using = ToStringSerializer.class)
   private Long faId;

   @NotNull(message = "文件类型不能为空")
   private Integer fileType;

}
