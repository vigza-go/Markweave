package com.vigza.markweave.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("doc")
@Data
public class Doc {

  private long id;
  private long docId;
  private String content;

}
