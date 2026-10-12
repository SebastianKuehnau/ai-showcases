package org.vaadin.demo.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Spring AI tools the chat assistant may call. Unlike the grid and chart controllers, the LLM does
 * not write SQL here: each tool is a fixed, parameterized query, and its (small) result is sent
 * back to the LLM so it can answer in prose.
 * <p>
 * Deliberately not a Spring bean: the MCP server auto-configuration that draiv-ai brings along
 * must not publish these tools to external agents.
 */
public class CustomerTools {

    private static final int MAX_RESULTS = 25;

    private final JdbcTemplate jdbc;

    public CustomerTools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Tool(description = "Find customers whose company name, contact name, city or country contains the "
            + "given text (case-insensitive). Returns at most 25 customers with key facts.")
    public List<Map<String, Object>> searchCustomers(
            @ToolParam(description = "Text to search for, e.g. 'Berlin' or 'Analytics'") String text) {
        String like = "%" + text.toLowerCase() + "%";
        return jdbc.queryForList("""
                SELECT ID, COMPANY_NAME, CONTACT_NAME, EMAIL, CITY, COUNTRY, ACTIVE,
                       CREDIT_SCORE, CUSTOMER_SINCE, LAST_ORDER_DATE
                FROM CUSTOMER
                WHERE LOWER(COMPANY_NAME) LIKE ? OR LOWER(CONTACT_NAME) LIKE ?
                   OR LOWER(CITY) LIKE ? OR LOWER(COUNTRY) LIKE ?
                ORDER BY COMPANY_NAME
                LIMIT ?""", like, like, like, like, MAX_RESULTS);
    }

    @Tool(description = "Sales summary of one customer: number of orders, total revenue (EUR, excluding "
            + "cancelled orders), first and last order date and revenue per product category.")
    public Map<String, Object> customerSalesSummary(
            @ToolParam(description = "Customer ID as returned by searchCustomers") long customerId) {
        Map<String, Object> summary = jdbc.queryForMap("""
                SELECT C.COMPANY_NAME, COUNT(O.ID) AS ORDERS, COALESCE(SUM(O.AMOUNT), 0) AS REVENUE_EUR,
                       MIN(O.ORDER_DATE) AS FIRST_ORDER, MAX(O.ORDER_DATE) AS LAST_ORDER
                FROM CUSTOMER C
                LEFT JOIN ORDERS O ON O.CUSTOMER_ID = C.ID AND O.STATUS <> 'Cancelled'
                WHERE C.ID = ?
                GROUP BY C.COMPANY_NAME""", customerId);
        summary.put("REVENUE_BY_CATEGORY", jdbc.queryForList("""
                SELECT PRODUCT_CATEGORY, SUM(AMOUNT) AS REVENUE_EUR
                FROM ORDERS WHERE CUSTOMER_ID = ? AND STATUS <> 'Cancelled'
                GROUP BY PRODUCT_CATEGORY ORDER BY REVENUE_EUR DESC""", customerId));
        return summary;
    }

    @Tool(description = "Top customers by revenue (EUR, excluding cancelled orders) within the last n months.")
    public List<Map<String, Object>> topCustomers(
            @ToolParam(description = "Look-back window in months, e.g. 12") int months,
            @ToolParam(description = "How many customers to return, at most 25") int limit) {
        return jdbc.queryForList("""
                SELECT C.ID, C.COMPANY_NAME, C.COUNTRY, SUM(O.AMOUNT) AS REVENUE_EUR, COUNT(*) AS ORDERS
                FROM ORDERS O JOIN CUSTOMER C ON C.ID = O.CUSTOMER_ID
                WHERE O.STATUS <> 'Cancelled' AND O.ORDER_DATE >= DATEADD('MONTH', ?, CURRENT_DATE)
                GROUP BY C.ID, C.COMPANY_NAME, C.COUNTRY
                ORDER BY REVENUE_EUR DESC
                LIMIT ?""", -Math.abs(months), Math.min(Math.max(limit, 1), MAX_RESULTS));
    }

    @Tool(description = "Open orders (status 'Open' or 'Shipped') with customer, date, category and amount.")
    public List<Map<String, Object>> openOrders() {
        return jdbc.queryForList("""
                SELECT O.ID, C.COMPANY_NAME, O.ORDER_DATE, O.PRODUCT_CATEGORY, O.AMOUNT, O.STATUS
                FROM ORDERS O JOIN CUSTOMER C ON C.ID = O.CUSTOMER_ID
                WHERE O.STATUS IN ('Open', 'Shipped')
                ORDER BY O.ORDER_DATE DESC
                LIMIT ?""", MAX_RESULTS);
    }
}
