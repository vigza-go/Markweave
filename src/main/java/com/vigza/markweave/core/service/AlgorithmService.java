package com.vigza.markweave.core.service;

import com.vigza.markweave.api.dto.Websocket.WsMessage;

import cn.hutool.json.JSONObject;

public interface AlgorithmService {
    void processOperation(Long docId, WsMessage<JSONObject> clientMsg);
}
