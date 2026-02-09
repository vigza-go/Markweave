package com.vigza.markweave.api.dto.FileSystem;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class FsNodeVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long docId;
    private String name;
    private String path;
    private Integer type;
    private String ownerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long size;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ptId;
}
