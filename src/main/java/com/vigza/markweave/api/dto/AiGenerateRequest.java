package com.vigza.markweave.api.dto;

import javax.validation.constraints.NotBlank;

public class AiGenerateRequest {
    @NotBlank
    private String prompt;

    private Double temperature;
    private Integer maxTokens;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}
