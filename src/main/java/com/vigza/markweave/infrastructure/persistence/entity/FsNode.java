package com.vigza.markweave.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("fs_node")
@Data
public class FsNode {

  private long id;
  private long userId;
  private long docId;
  private String name;
  private long faId;
  private String path;
  private long isFolder;


}
