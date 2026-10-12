package org.vaadin.demo.views.navigation;

import com.vaadin.draiv.ai.ui.AiActionButton;
import com.vaadin.draiv.ai.ui.UiAssistant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.demo.views.MainLayout;

/**
 * Explains AI navigation and offers one-click examples. Each example is a draiv
 * {@link AiActionButton} with a fixed request, run through the same goal as the
 * {@link AiNavigationBar} in the navbar.
 */
@Route(value = "navigation", layout = MainLayout.class)
@PageTitle("AI Navigation")
@Menu(order = 6, title = "AI Navigation", icon = "vaadin:compass")
public class AiNavigationView extends VerticalLayout {

    public AiNavigationView(UiAssistant assistant) {
        addClassName("ai-nav-view");
        setMaxWidth("900px");

        add(new H1("AI Navigation"),
                new Paragraph("Type where you want to go into the bar at the top — on any page. The AI "
                        + "(draiv) inspects the live UI on the server, navigates to the matching route "
                        + "and can then continue on the target view: fill the customer search, or hand "
                        + "your request to that view's own AI."),
                howItWorks(),
                new H3("Try it"));

        add(example(assistant, "Open the form to add a new customer"),
                example(assistant, "Show me active customers from Berlin"),
                example(assistant, "Show revenue by product category as a pie chart"),
                example(assistant, "I want a table of all open orders"),
                example(assistant, "Ask the assistant who our top 3 customers are"));
    }

    private static AiActionButton example(UiAssistant assistant, String request) {
        AiActionButton button = new AiActionButton(assistant, request, () -> AiNavigationBar.goal(request));
        button.setSuccessMessage("Done: " + request);
        return button;
    }

    private static Div howItWorks() {
        OrderedList steps = new OrderedList(
                new ListItem("The request plus a list of the app's routes goes to the LLM as a draiv goal."),
                new ListItem("The LLM calls draiv's navigate tool; draiv validates the route against "
                        + "Vaadin's route registry and returns a snapshot of the new view."),
                new ListItem("If the request contains a task, the LLM operates the new view through "
                        + "draiv's capability tools — e.g. submitting the prompt to the chart's AI input."),
                new ListItem("Everything runs server-side against the live UI; @Push streams the changes "
                        + "to the browser."));
        Div card = new Div(steps);
        card.addClassName("ai-nav-view__steps");
        return card;
    }
}
