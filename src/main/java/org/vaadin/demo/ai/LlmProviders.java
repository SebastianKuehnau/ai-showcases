package org.vaadin.demo.ai;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.SpringAILLMProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Hands out Vaadin {@link LLMProvider}s backed by Spring AI's OpenAI chat model.
 * <p>
 * Every {@code AIOrchestrator} needs its own provider instance (Vaadin rejects sharing one between
 * orchestrators), so views call {@link #create()} once per orchestrator. The concrete
 * {@link OpenAiChatModel} is injected rather than {@code ChatModel}, because draiv-ai also brings the
 * Mistral and Anthropic starters and the generic type would be ambiguous.
 */
@Service
public class LlmProviders {

    private static final String NOT_CONFIGURED = "not-configured";

    private final OpenAiChatModel chatModel;
    private final boolean configured;

    public LlmProviders(OpenAiChatModel chatModel,
                        @Value("${spring.ai.openai.api-key:" + NOT_CONFIGURED + "}") String apiKey) {
        this.chatModel = chatModel;
        this.configured = apiKey != null && !apiKey.isBlank() && !NOT_CONFIGURED.equals(apiKey);
    }

    /** {@code false} when no {@code OPENAI_API_KEY} was supplied; views then disable their AI input. */
    public boolean isConfigured() {
        return configured;
    }

    public LLMProvider create() {
        return new SpringAILLMProvider(chatModel);
    }
}
