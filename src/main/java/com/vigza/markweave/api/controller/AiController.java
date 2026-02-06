package com.vigza.markweave.api.controller;

import com.vigza.markweave.api.dto.AiGenerateRequest;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.core.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/generate")
    public Result<String> generate(@Valid @RequestBody AiGenerateRequest request) {
        return Result.success(aiService.generate(request));
    }
}
