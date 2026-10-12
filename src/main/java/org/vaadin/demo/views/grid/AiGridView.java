package org.vaadin.demo.views.grid;

import com.vaadin.flow.component.ai.grid.AIDataRow;
import com.vaadin.flow.component.ai.grid.GridAIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.demo.ai.H2DatabaseProvider;
import org.vaadin.demo.ai.LlmProviders;
import org.vaadin.demo.views.AiAssistantPanel;
import org.vaadin.demo.views.AiShowcaseLayout;
import org.vaadin.demo.views.MainLayout;

/**
 * AI-generated grid: the LLM inspects the schema and writes a SQL query; {@link GridAIController}
 * validates it and renders the result with generated columns. The rows never go back to the LLM.
 */
@Route(value = "grid", layout = MainLayout.class)
@PageTitle("AI-Generated Grid")
@Menu(order = 4, title = "AI Grid", icon = "vaadin:table")
public class AiGridView extends AiShowcaseLayout {

    private static final String SYSTEM_PROMPT = """
            You build tables of our sales data for business users. Prefer readable column names, \
            sort results meaningfully (e.g. largest revenue first) and format money in EUR. \
            Briefly confirm in one sentence what the grid now shows.
            """;

    public AiGridView(LlmProviders llmProviders, H2DatabaseProvider databaseProvider) {
        this(new Grid<>(), llmProviders, databaseProvider);
    }

    private AiGridView(Grid<AIDataRow> grid, LlmProviders llmProviders, H2DatabaseProvider databaseProvider) {
        super(grid, new AiAssistantPanel("AI-Generated Grid",
                "Describe the table you need. The AI writes the query, the grid builds its own columns.",
                llmProviders.isConfigured()));
        grid.addThemeVariants(GridVariant.ROW_STRIPES);
        grid.setSizeFull();

        AiAssistantPanel panel = getPanel();
        if (llmProviders.isConfigured()) {
            AIOrchestrator orchestrator = AIOrchestrator.builder(llmProviders.create(), SYSTEM_PROMPT)
                    .withMessageList(panel.getMessageList())
                    .withInput(panel.getInput())
                    .withController(new GridAIController(grid, databaseProvider))
                    .build();
            panel.setExamples(orchestrator::prompt,
                    "Top 10 customers by revenue in the last 12 months, with country and number of orders",
                    "Monthly revenue per product category for this year",
                    "All open orders with company name, contact and amount",
                    "Customers grouped under 'Customer' (name, city) and 'Sales' (orders, revenue)");
        }
    }
}
