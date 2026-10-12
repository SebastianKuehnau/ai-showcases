package org.vaadin.demo.views.chat;

import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.jdbc.core.JdbcTemplate;
import org.vaadin.demo.ai.CustomerTools;
import org.vaadin.demo.ai.LlmProviders;
import org.vaadin.demo.views.AiAssistantPanel;
import org.vaadin.demo.views.MainLayout;

/**
 * Classic AI chat: Message List + Message Input wired by the {@link AIOrchestrator}, streaming the
 * answer token by token, with Spring AI tools that look up customers and orders.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("AI Chat")
@Menu(order = 0, title = "AI Chat", icon = "vaadin:chat")
public class ChatView extends VerticalLayout {

    private static final String SYSTEM_PROMPT = """
            You are the sales assistant of a B2B company. Answer questions about our customers and \
            their orders using the provided tools; never invent customers, figures or dates. \
            Amounts are in EUR. Answer concisely in the user's language and use Markdown tables \
            for lists of more than three items.
            """;

    public ChatView(LlmProviders llmProviders, JdbcTemplate jdbcTemplate) {
        AiAssistantPanel chat = new AiAssistantPanel("AI Chat",
                "Ask about customers and orders — the assistant calls server-side tools to look them up.",
                llmProviders.isConfigured());
        chat.addClassName("chat-panel");
        chat.setSizeFull();

        if (llmProviders.isConfigured()) {
            AIOrchestrator orchestrator = AIOrchestrator.builder(llmProviders.create(), SYSTEM_PROMPT)
                    .withMessageList(chat.getMessageList())
                    .withInput(chat.getInput())
                    .withTools(new CustomerTools(jdbcTemplate))
                    .withAssistantName("Sales Assistant")
                    .build();
            chat.setExamples(orchestrator::prompt,
                    "Who are our top 5 customers in the last 12 months?",
                    "Which orders are still open?",
                    "Give me a sales summary for Berlin Data Works.",
                    "Welche Kunden haben wir in Hamburg?");
        }

        add(chat);
        setSizeFull();
        setPadding(false);
    }
}
