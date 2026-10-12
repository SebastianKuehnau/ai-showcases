package org.vaadin.demo.ai;

import com.vaadin.flow.component.ai.provider.DatabaseProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Bridges the AI grid and chart controllers to the demo database.
 * <p>
 * The LLM writes the SQL that runs here, so queries go through the read-only {@code AI_READER}
 * account created in {@code data.sql} (SELECT on CUSTOMER and ORDERS only) — never through the
 * application's own data source. Query results are rendered in the Grid or Chart; they are not
 * sent back to the LLM. SQL errors are rethrown as {@link ToolException} so the LLM sees the H2
 * message and can correct its query in the same turn.
 */
@Component
public class H2DatabaseProvider implements DatabaseProvider {

    private static final int MAX_ROWS = 5_000;
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private static final String SCHEMA = """
            Dialect: H2 2.x (SQL standard; use EXTRACT(YEAR FROM d), EXTRACT(MONTH FROM d), \
            FORMATDATETIME(d, 'yyyy-MM') for month labels, DATEADD('MONTH', -n, CURRENT_DATE), \
            LIMIT n). Only SELECT statements are allowed. VALUE, YEAR, MONTH, KEY are reserved words: \
            quote them in double quotes when used as identifiers or aliases.

            Table CUSTOMER — one row per customer company:
              ID BIGINT primary key
              COMPANY_NAME VARCHAR
              CONTACT_NAME VARCHAR
              ACTIVE BOOLEAN — whether the customer relationship is active
              ANNUAL_REVENUE DECIMAL — the customer's own annual revenue in EUR (company size, not our sales)
              CREDIT_SCORE INT — normalized 0–100; rating: >= 70 'Creditworthy', 40–69 'Limited', < 40 'At risk'
              CUSTOMER_SINCE DATE
              LAST_ORDER_DATE DATE
              EMAIL VARCHAR, PHONE VARCHAR
              STREET VARCHAR, HOUSE_NUMBER VARCHAR, POSTAL_CODE VARCHAR, CITY VARCHAR, STATE VARCHAR
              COUNTRY VARCHAR — English country name, e.g. 'Germany', 'France', 'United Kingdom'
              COUNTRY_CODE VARCHAR — ISO 3166 alpha-2, e.g. 'DE'

            Table ORDERS — one row per order we received, covering roughly the last 24 months:
              ID BIGINT primary key
              CUSTOMER_ID BIGINT — references CUSTOMER.ID
              ORDER_DATE DATE
              AMOUNT DECIMAL — order value in EUR (this is our sales revenue)
              QUANTITY INT
              PRODUCT_CATEGORY VARCHAR — one of 'Software', 'Hardware', 'Consulting', 'Support', 'Training'
              SALES_CHANNEL VARCHAR — one of 'Online', 'Partner', 'Direct'
              STATUS VARCHAR — one of 'Open', 'Shipped', 'Delivered', 'Cancelled'

            Business rules: "revenue" or "sales" means SUM(ORDERS.AMOUNT) excluding STATUS = 'Cancelled' \
            unless the user asks otherwise. Use readable column aliases (e.g. "Revenue (EUR)").
            """;

    private final transient JdbcTemplate jdbc;

    public H2DatabaseProvider(@Value("${ai.database.url}") String url,
                              @Value("${ai.database.username}") String username,
                              @Value("${ai.database.password}") String password) {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        jdbc.setMaxRows(MAX_ROWS);
        jdbc.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
    }

    @Override
    public String getSchema() {
        return SCHEMA;
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        try {
            return jdbc.queryForList(sql);
        } catch (DataAccessException e) {
            throw new ToolException("Query failed: " + e.getMostSpecificCause().getMessage(), e);
        }
    }
}
