package com.vigza.markweave.api.controller;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigza.markweave.common.Result;
import com.vigza.markweave.core.service.MarkdownAiServiceImpl;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private MarkdownAiServiceImpl aiService;

    @PostMapping("/format")
    public Result<String> formatMarkdown(@RequestHeader("Authorization") String token,@Valid @NotNull @RequestBody Long docId) {
        return aiService.smartFormat(token, docId);
    }
}