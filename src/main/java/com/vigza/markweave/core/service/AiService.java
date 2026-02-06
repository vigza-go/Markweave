package com.vigza.markweave.core.service;

import com.vigza.markweave.api.dto.AiGenerateRequest;

public interface AiService {
    String generate(AiGenerateRequest request);
}
