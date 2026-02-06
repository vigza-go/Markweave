package com.vigza.markweave.core.service;

import com.vigza.markweave.common.Result;

public interface MarkdownAiService {
    Result<String> smartFormat(String token,Long docId);
}
