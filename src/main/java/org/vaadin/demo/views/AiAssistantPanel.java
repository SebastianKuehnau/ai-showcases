package org.vaadin.demo.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.function.Consumer;

/**
 * Side panel shared by the AI views: a title, a hint, the conversation, clickable example prompts
 * and the prompt input. The view passes {@link #getMessageList()} and {@link #getInput()} to its
 * {@code AIOrchestrator}; the panel itself knows nothing about the AI.
 */
public class AiAssistantPanel extends VerticalLayout {

    private final MessageList messageList = new MessageList();
    private final MessageInput input = new MessageInput();
    private final Div examples = new Div();
    private final boolean aiEnabled;

    public AiAssistantPanel(String title, String hint, boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
        addClassName("ai-panel");
        setPadding(true);
        setSpacing(true);

        add(new H3(title), new Paragraph(hint));

        messageList.setMarkdown(true);
        messageList.addClassName("ai-panel__messages");

        examples.addClassName("ai-panel__examples");

        input.setWidthFull();
        input.setEnabled(aiEnabled);
        if (!aiEnabled) {
            Span warning = new Span("AI disabled — start the app with OPENAI_API_KEY set.");
            warning.addClassName("ai-panel__disabled");
            add(warning);
        }

        add(messageList, examples, input);
        setFlexGrow(1, messageList);
    }

    /** Adds one-click example prompts; clicking one sends it through {@code sender}. */
    public void setExamples(Consumer<String> sender, String... prompts) {
        examples.removeAll();
        for (String prompt : prompts) {
            Button example = new Button(prompt, _ -> sender.accept(prompt));
            example.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
            example.setEnabled(aiEnabled);
            examples.add(example);
        }
    }

    /** Places an extra control (e.g. an upload button) directly above the input. */
    public void addAboveInput(Component component) {
        addComponentAtIndex(indexOf(input), component);
    }

    public MessageList getMessageList() {
        return messageList;
    }

    public MessageInput getInput() {
        return input;
    }
}
