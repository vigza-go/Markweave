package com.vigza.markweave.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentDocVO {
    private Long docId;
    private String docName;
    private Long ownerId;
    private String ownerName;
    private Integer role;
    private Long size;
    private LocalDateTime lastViewTime;
}
