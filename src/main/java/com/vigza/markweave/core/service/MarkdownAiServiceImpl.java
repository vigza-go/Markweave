package com.vigza.markweave.core.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vigza.markweave.common.Result;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.service.RedisService;

import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletionResponse;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.PlatformType;
import io.github.lnyocly.ai4j.service.factor.AiService;

@Service
public class MarkdownAiServiceImpl implements MarkdownAiService {

    @Autowired
    private AiService aiService;

    @Autowired
    private FileSystemService fileSystemService;

    @Autowired
    private CollaborationService collaborationService;

    @Override
    public Result<String> smartFormat(String token, Long docId) {

        if (!collaborationService.canWrite(token, docId)) {
            return Result.error(403, "无权限");
        }

        String content = fileSystemService.getDocContent(docId);

        // 1. 定义 Prompt 模板，明确要求美化 Markdown 排版并修正语法
        String template = "你是一个专业的 Markdown 排版专家。请对以下 Markdown 内容进行美化和修正：\n" +
                "1. 修正语法错误和错别字。\n" +
                "2. 优化标题层级和段落间距。\n" +
                "3. 确保代码块、链接、图片格式正确。\n" +
                "4. 请仅返回修正后的 Markdown 内容，不要包含任何解释性文字。\n" +
                "\n" +
                "待处理内容：\n" +
                content;

        // 获取OLLAMA的聊天服务
        IChatService chatService = aiService.getChatService(PlatformType.OLLAMA);

        // 创建请求参数
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("deepseek-r1:32b")
                .message(ChatMessage.withUser(template))
                .build();

        // 2. 调用 AI 模型
        ChatCompletionResponse chatCompletionResponse = null;
        try {
            chatCompletionResponse = chatService.chatCompletion(chatCompletion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 获取聊天内容和token消耗
        String aiResult = chatCompletionResponse.getChoices().get(0).getMessage().getContent();
        long totalTokens = chatCompletionResponse.getUsage().getTotalTokens();
        System.out.println("总token消耗: " + totalTokens);
        
        return Result.success(aiResult);
    }

}
