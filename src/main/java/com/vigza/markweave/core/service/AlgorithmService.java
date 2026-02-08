package com.vigza.markweave.core.service;

import cn.hutool.json.JSONObject;

public interface AlgorithmService {
    void processOperation(Long docId, JSONObject clientMsg);
}
