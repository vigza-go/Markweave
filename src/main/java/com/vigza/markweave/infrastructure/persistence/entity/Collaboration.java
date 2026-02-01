package com.vigza.markweave.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.annotation.AccessType;

@TableName("collaboration")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Collaboration {
  private Long id;
  private Long userId;
  private Long docId;
  private Integer role;
  private LocalDateTime createTime;
  private LocalDateTime lastViewTime;

}
