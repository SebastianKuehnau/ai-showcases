package org.vaadin.demo.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * A structured description of "which customers to show" — the value the query-builder UI
 * produces and hands to {@link CustomerSpecifications}. Every field is optional; a {@code null}
 * (or empty collection) means "don't constrain on this dimension".
 * <p>
 * This is deliberately a plain, UI-agnostic model. Today the {@code CustomerSearchForm} fills it
 * in; later the AI search is meant to drive the same form (and thus produce the same object), so
 * that a user can see how a natural-language query maps onto concrete filters and continue from
 * where the LLM left off.
 */
public record CustomerFilter(
        String text,
        String companyName,
        String contactName,
        String email,
        String city,
        Set<String> countries,
        Boolean active,
        Set<CreditRating> creditRatings,
        BigDecimal minRevenue,
        BigDecimal maxRevenue,
        LocalDate customerSinceFrom,
        LocalDate customerSinceTo,
        LocalDate lastOrderFrom,
        LocalDate lastOrderTo) {

    /** An empty filter that matches every customer. */
    public static CustomerFilter empty() {
        return new CustomerFilter(null, null, null, null, null, Set.of(), null, Set.of(),
                null, null, null, null, null, null);
    }
}
