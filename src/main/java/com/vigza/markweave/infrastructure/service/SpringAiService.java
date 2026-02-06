package com.vigza.markweave.infrastructure.service;

import com.vigza.markweave.api.dto.AiGenerateRequest;
import com.vigza.markweave.core.service.AiService;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SpringAiService implements AiService {
    private final ChatModel chatModel;

    @Autowired
    public SpringAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generate(AiGenerateRequest request) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .build();
        Prompt prompt = new Prompt(Collections.singletonList(new UserMessage(request.getPrompt())), options);
        ChatResponse response = chatModel.call(prompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getContent();
    }
}
