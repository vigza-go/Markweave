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
public class RecentDocVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long docId;
    private String docName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;
    private String ownerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long size;
    private LocalDateTime lastViewTime;
}
