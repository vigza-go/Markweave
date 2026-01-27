package com.vigza.markweave.infrastructure.persistence.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("doc_meta")
@Data
public class DocMeta {

  private long id;
  private String docname;
  private long contentId;
  private long size;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  private long isPublic;
  private long isDeleted;


}
