package com.vigza.markweave.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("fs_node")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FsNode {

  private Long id;
  private Long userId;
  private String userName;
  private Long docId;
  private String name;
  private Long faId;
  private String path;
  private Integer type;
  private Long ptId;
  private Boolean recycled;
  private Long size;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

}
