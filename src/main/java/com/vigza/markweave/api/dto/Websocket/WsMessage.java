package com.vigza.markweave.api.dto.Websocket;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsMessage<T> {
    @JsonSerialize(using = ToStringSerializer.class)
    Long docId;
    String method;
    String clientId;
    @JsonSerialize(using = ToStringSerializer.class)
    Long msgId;
    @JsonSerialize(using = ToStringSerializer.class)
    Long version;
    Integer retryCount;
    // 3态：msg(错误信息) text(fullText) op([op1,op2,...])
    T data;
}
