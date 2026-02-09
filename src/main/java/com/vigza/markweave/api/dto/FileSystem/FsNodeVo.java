package com.vigza.markweave.api.dto.FileSystem;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FsNodeVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String docOwner;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long docId;
    private String name;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long faId;
    private String path;
    private Integer type;
    private String ownerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ptId;
    private Boolean recycled;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long size;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private LocalDateTime lastViewTime;
}
