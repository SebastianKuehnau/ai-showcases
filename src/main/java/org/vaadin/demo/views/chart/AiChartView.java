package org.vaadin.demo.views.chart;

import com.vaadin.flow.component.ai.chart.ChartAIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.demo.ai.H2DatabaseProvider;
import org.vaadin.demo.ai.LlmProviders;
import org.vaadin.demo.views.AiAssistantPanel;
import org.vaadin.demo.views.AiShowcaseLayout;
import org.vaadin.demo.views.MainLayout;

/**
 * AI-generated chart: the LLM writes the series queries and the Highcharts configuration;
 * {@link ChartAIController} validates both and applies them together at the end of the turn.
 */
@Route(value = "chart", layout = MainLayout.class)
@PageTitle("AI-Generated Chart")
@Menu(order = 5, title = "AI Chart", icon = "vaadin:chart")
public class AiChartView extends AiShowcaseLayout {

    private static final String SYSTEM_PROMPT = """
            You build charts of our sales data for business users. Always set a chart title and \
            axis titles with units (EUR), order time axes chronologically and keep follow-up \
            requests ("make it a pie chart", "only 2026") on the current chart. \
            Briefly confirm in one sentence what the chart now shows.
            """;

    public AiChartView(LlmProviders llmProviders, H2DatabaseProvider databaseProvider) {
        this(new Chart(), llmProviders, databaseProvider);
    }

    private AiChartView(Chart chart, LlmProviders llmProviders, H2DatabaseProvider databaseProvider) {
        super(chart, new AiAssistantPanel("AI-Generated Chart",
                "Describe the chart you want, then refine it step by step.",
                llmProviders.isConfigured()));
        chart.setSizeFull();

        AiAssistantPanel panel = getPanel();
        if (llmProviders.isConfigured()) {
            AIOrchestrator orchestrator = AIOrchestrator.builder(llmProviders.create(), SYSTEM_PROMPT)
                    .withMessageList(panel.getMessageList())
                    .withInput(panel.getInput())
                    .withController(new ChartAIController(chart, databaseProvider))
                    .build();
            panel.setExamples(orchestrator::prompt,
                    "Monthly revenue of the last 12 months as a column chart",
                    "Revenue by product category as a pie chart",
                    "Compare monthly revenue per sales channel as a stacked area chart",
                    "Number of customers per country as a bar chart");
        }
    }
}
