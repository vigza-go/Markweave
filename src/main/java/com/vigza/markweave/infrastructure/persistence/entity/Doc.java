package com.vigza.markweave.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;

@TableName("doc")
@Data
@AllArgsConstructor
public class Doc {

  private Long id;
  private String content;

}
