package org.vaadin.demo.views.navigation;

import com.vaadin.draiv.ai.ui.UiAssistant;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * App-wide "where do you want to go?" bar in the navbar. The user's words go to draiv's
 * {@link UiAssistant}, whose capability tools inspect the live UI, call {@code navigate} (which
 * validates the route against Vaadin's route registry) and can then operate the target view —
 * including submitting a prompt to that view's own AI input.
 * <p>
 * Built directly on {@link UiAssistant#assist(String)} instead of draiv's {@code AiGoalBar}: the
 * goal bar registers a global Enter shortcut, which in an always-present navbar would steal Enter
 * from every view. Here Enter is scoped to the prompt field.
 */
public class AiNavigationBar extends HorizontalLayout {

    private static final String GOAL = """
            The user wants to go somewhere in this application. Navigate to the view that best \
            matches the request, using one of these routes:
            - "" (empty, root): AI Chat — questions about customers and orders
            - "customers": Customers — customer search form above a customer grid
            - "form": AI Form Filler — capture a new customer record from text
            - "form/receipt": AI Form Filler (Image) — fill an expense claim from a receipt image
            - "grid": AI Grid — a table generated from sales data (customers, orders, revenue)
            - "chart": AI Chart — a chart generated from sales data
            - "navigation": AI Navigation — explains this navigation feature

            If the request also contains a task for the target view, do it after navigating:
            - Customers: set only the matching search form fields, then click the Search button;
            - Chat, Form Fillers, Grid, Chart: type the task into the message input at the bottom \
            of the assistant panel and submit it (that view's own AI then carries it out).
            Do nothing beyond that. Never use the navigation prompt field in the top bar. \
            Request: "%s"
            """;

    private final UiAssistant assistant;
    private final TextField prompt = new TextField();
    private final Button go = new Button("Go", VaadinIcon.COMPASS.create());

    public AiNavigationBar(UiAssistant assistant) {
        this.assistant = assistant;
        addClassName("ai-nav-bar");

        prompt.setPlaceholder("Where to? e.g. \"show revenue by category as a pie chart\"");
        prompt.setClearButtonVisible(true);
        prompt.setPrefixComponent(VaadinIcon.MAGIC.create());
        prompt.setAriaLabel("AI navigation");
        prompt.addKeyPressListener(Key.ENTER, _ -> run());
        prompt.setWidthFull();

        go.addThemeVariants(ButtonVariant.PRIMARY);
        go.addClickListener(_ -> run());

        if (!assistant.isEnabled()) {
            prompt.setEnabled(false);
            go.setEnabled(false);
            prompt.setTooltipText(assistant.disabledReason());
        }

        add(prompt, go);
        setFlexGrow(1, prompt);
        setAlignItems(Alignment.CENTER);
    }

    /** The full draiv goal for a navigation request, shared with the example buttons. */
    public static String goal(String request) {
        return GOAL.formatted(request);
    }

    private void run() {
        String request = prompt.getValue().trim();
        if (request.isEmpty()) {
            return;
        }
        UI ui = UI.getCurrent();
        setBusy(true);
        // assist() runs the model + tool loop off the UI thread; re-enter via ui.access (needs @Push)
        assistant.assist(goal(request)).whenComplete((result, error) -> ui.access(() -> {
            setBusy(false);
            if (error != null) {
                Notification.show("AI navigation failed: " + error.getMessage());
            } else {
                prompt.clear();
            }
        }));
    }

    private void setBusy(boolean busy) {
        prompt.setEnabled(!busy);
        go.setEnabled(!busy);
        go.setText(busy ? "Navigating…" : "Go");
    }
}
