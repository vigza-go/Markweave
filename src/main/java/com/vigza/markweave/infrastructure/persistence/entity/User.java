package com.vigza.markweave.infrastructure.persistence.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("user")
@Data
public class User {

  private Long id;
  private String account;
  private String password;
  private String salt;
  private String nickname;
  private String headUrl;
  private LocalDateTime createTime;
  private Integer type;
  private Long userSpaceNodeId;
  private Long userShareSpaceNodeId;
}
