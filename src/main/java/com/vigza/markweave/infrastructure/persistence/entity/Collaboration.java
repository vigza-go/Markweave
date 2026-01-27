package com.vigza.markweave.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("collaboration")
@Data
public class Collaboration {
  private long id;
  private long userId;
  private long docId;
  private long role;
  private LocalDateTime createTime;
  private LocalDateTime lastViewTime;

}
