package org.vaadin.demo.data;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a {@link CustomerFilter} into a JPA {@link Specification}, so the query-builder UI
 * filters at the database level via {@code CustomerRepository.findAll(spec)} rather than in memory.
 * <p>
 * Kept separate from the UI on purpose: the mapping from "filter criteria" to "SQL predicates" is
 * the reusable, testable core that both the manual form and (later) the AI-driven search share.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> matching(CustomerFilter f) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            Path<?> address = root.get("address");

            // Free text: match against any of the prominent text columns.
            if (hasText(f.text())) {
                String like = anywhere(f.text());
                and.add(cb.or(
                        contains(cb, root.get("companyName"), like),
                        contains(cb, root.get("contactName"), like),
                        contains(cb, root.get("email"), like),
                        contains(cb, root.get("phone"), like),
                        contains(cb, address.get("city"), like),
                        contains(cb, address.get("street"), like),
                        contains(cb, address.get("country"), like)));
            }

            // Per-field "contains" text filters.
            if (hasText(f.companyName())) {
                and.add(contains(cb, root.get("companyName"), anywhere(f.companyName())));
            }
            if (hasText(f.contactName())) {
                and.add(contains(cb, root.get("contactName"), anywhere(f.contactName())));
            }
            if (hasText(f.email())) {
                and.add(contains(cb, root.get("email"), anywhere(f.email())));
            }
            if (hasText(f.city())) {
                and.add(contains(cb, address.get("city"), anywhere(f.city())));
            }

            // Country: any of the selected values.
            if (f.countries() != null && !f.countries().isEmpty()) {
                and.add(address.get("country").in(f.countries()));
            }

            // Active flag (null = either).
            if (f.active() != null) {
                and.add(cb.equal(root.get("active"), f.active()));
            }

            // Credit rating: OR the score band of each selected rating.
            if (f.creditRatings() != null && !f.creditRatings().isEmpty()) {
                Path<Integer> score = root.get("creditScore");
                List<Predicate> ratingOr = new ArrayList<>();
                for (CreditRating rating : f.creditRatings()) {
                    ratingOr.add(cb.between(score, rating.minScoreInclusive(), rating.maxScoreInclusive()));
                }
                and.add(cb.or(ratingOr.toArray(Predicate[]::new)));
            }

            // Annual revenue range.
            if (f.minRevenue() != null) {
                and.add(cb.greaterThanOrEqualTo(root.get("annualRevenue"), f.minRevenue()));
            }
            if (f.maxRevenue() != null) {
                and.add(cb.lessThanOrEqualTo(root.get("annualRevenue"), f.maxRevenue()));
            }

            // Customer-since date range.
            if (f.customerSinceFrom() != null) {
                and.add(cb.greaterThanOrEqualTo(root.get("customerSince"), f.customerSinceFrom()));
            }
            if (f.customerSinceTo() != null) {
                and.add(cb.lessThanOrEqualTo(root.get("customerSince"), f.customerSinceTo()));
            }

            // Last-order date range.
            if (f.lastOrderFrom() != null) {
                and.add(cb.greaterThanOrEqualTo(root.get("lastOrderDate"), f.lastOrderFrom()));
            }
            if (f.lastOrderTo() != null) {
                and.add(cb.lessThanOrEqualTo(root.get("lastOrderDate"), f.lastOrderTo()));
            }

            return and.isEmpty() ? cb.conjunction() : cb.and(and.toArray(Predicate[]::new));
        };
    }

    private static Predicate contains(jakarta.persistence.criteria.CriteriaBuilder cb, Path<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern);
    }

    private static String anywhere(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
