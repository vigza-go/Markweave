package com.vigza.markweave.api.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FileNodeVo {
    private Long id;
    private String name;
    private Integer type;
    private String ownerName;
    private Long size;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private Long ptId;
}