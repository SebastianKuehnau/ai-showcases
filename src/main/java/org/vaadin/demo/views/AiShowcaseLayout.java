package org.vaadin.demo.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * Two-column view body: the AI-driven component (form, grid, chart) on the left, the
 * {@link AiAssistantPanel} on the right. Stacks vertically on narrow screens (see styles.css).
 */
public class AiShowcaseLayout extends Div {

    private final AiAssistantPanel panel;

    public AiShowcaseLayout(Component main, AiAssistantPanel panel) {
        this.panel = panel;
        addClassName("ai-showcase");
        main.addClassName("ai-showcase__main");
        add(main, panel);
        setSizeFull();
    }

    protected AiAssistantPanel getPanel() {
        return panel;
    }
}
