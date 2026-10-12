package org.vaadin.demo.views.customers;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import org.vaadin.demo.data.CreditRating;
import org.vaadin.demo.data.CustomerFilter;

import java.util.List;
import java.util.function.Consumer;

/**
 * A "query builder" for {@link org.vaadin.demo.data.Customer}: a fairly comprehensive
 * form whose fields map onto the filterable dimensions of the entity. Pressing <em>Search</em>
 * collects the field values into an immutable {@link CustomerFilter} and hands it to the supplied
 * callback.
 * <p>
 * The form is intentionally the single, explicit place where "what the user is searching for" is
 * expressed. The AI search fills in these very fields, so a natural-language query becomes visible,
 * editable structured criteria the user can refine and continue from. The fields the AI changed
 * are indicated automatically by the capability layer (see
 * {@link com.vaadin.flow.automation.indication.ChangeIndication}) — no change tracking here.
 */
public class CustomerSearchForm extends FormLayout {

    private final TextField companyName = new TextField("Company name");
    private final TextField contactName = new TextField("Contact name");
    private final TextField email = new TextField("Email");
    private final TextField city = new TextField("City");
    private final MultiSelectComboBox<String> countries = new MultiSelectComboBox<>("Country");
    private final Select<ActiveStatus> active = new Select<>();
    private final MultiSelectComboBox<CreditRating> creditRatings = new MultiSelectComboBox<>("Credit rating");
    private final NumberRange revenue = new NumberRange("Annual revenue (€)");
    private final DateRange customerSince = new DateRange("Customer since");
    private final DateRange lastOrder = new DateRange("Last order");

    private final Consumer<CustomerFilter> onSearch;

    /** Tri-state for the boolean {@code active} flag, so "no preference" is expressible. */
    private enum ActiveStatus {
        ANY("Any"), ACTIVE("Active only"), INACTIVE("Inactive only");

        final String label;

        ActiveStatus(String label) {
            this.label = label;
        }

        Boolean toFilterValue() {
            return this == ANY ? null : this == ACTIVE;
        }
    }

    public CustomerSearchForm(List<String> availableCountries, Consumer<CustomerFilter> onSearch) {
        this.onSearch = onSearch;

        countries.setItems(availableCountries);
        countries.setClearButtonVisible(true);

        active.setLabel("Status");
        active.setItems(ActiveStatus.values());
        active.setItemLabelGenerator(status -> status.label);
        active.setValue(ActiveStatus.ANY);

        creditRatings.setItems(CreditRating.values());
        creditRatings.setItemLabelGenerator(CreditRating::getLabel);
        creditRatings.setClearButtonVisible(true);

        // Stable ids so the AI search can target each field precisely when it
        // inspects the live component tree and fills the form.
        companyName.setId("filter-company-name");
        contactName.setId("filter-contact-name");
        email.setId("filter-email");
        city.setId("filter-city");
        countries.setId("filter-countries");
        active.setId("filter-status");
        creditRatings.setId("filter-credit-rating");
        revenue.from.setId("filter-revenue-min");
        revenue.to.setId("filter-revenue-max");
        customerSince.from.setId("filter-customer-since-from");
        customerSince.to.setId("filter-customer-since-to");
        lastOrder.from.setId("filter-last-order-from");
        lastOrder.to.setId("filter-last-order-to");

        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("500px", 2),
                new ResponsiveStep("900px", 3));

        add(companyName, contactName, email, city, countries, active, creditRatings,
                revenue, customerSince, lastOrder);

        add(new ButtonBar());
    }

    /** Search / Reset actions, spanning the full form width. */
    private class ButtonBar extends HorizontalLayout {
        ButtonBar() {
            Button search = new Button("Search", _ -> onSearch.accept(buildFilter()));
            search.setId("search-button");
            search.addThemeVariants(ButtonVariant.PRIMARY);
            search.addClickShortcut(Key.ENTER);

            Button reset = new Button("Reset", _ -> reset());

            add(search, reset);
            setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            setWidthFull();
            getStyle().setMarginTop("var(--vaadin-gap-m, 1rem)");
            CustomerSearchForm.this.setColspan(this, 3);
        }
    }

    private CustomerFilter buildFilter() {
        return new CustomerFilter(
                null, // free-text "search anything" removed; the AI search covers free-form queries
                companyName.getValue(),
                contactName.getValue(),
                email.getValue(),
                city.getValue(),
                countries.getValue(),
                active.getValue().toFilterValue(),
                creditRatings.getValue(),
                revenue.from.getValue(),
                revenue.to.getValue(),
                customerSince.from.getValue(),
                customerSince.to.getValue(),
                lastOrder.from.getValue(),
                lastOrder.to.getValue());
    }

    /** Clears every field back to "no constraint" and re-runs the (now empty) search. */
    public void reset() {
        companyName.clear();
        contactName.clear();
        email.clear();
        city.clear();
        countries.clear();
        active.setValue(ActiveStatus.ANY);
        creditRatings.clear();
        revenue.clear();
        customerSince.clear();
        lastOrder.clear();
        onSearch.accept(CustomerFilter.empty());
    }

    /** A "min – max" pair of numeric inputs shown as one form item. */
    private static class NumberRange extends HorizontalLayout {
        final BigDecimalField from = new BigDecimalField();
        final BigDecimalField to = new BigDecimalField();

        NumberRange(String label) {
            from.setLabel(label);
            from.setPlaceholder("min");
            to.setPlaceholder("max");
            from.setWidthFull();
            to.setWidthFull();
            setWidthFull();
            add(from, to);

            setAlignItems(Alignment.BASELINE);
        }

        void clear() {
            from.clear();
            to.clear();
        }
    }

    /** A "from – until" pair of date pickers shown as one form item. */
    private static class DateRange extends HorizontalLayout {
        final DatePicker from = new DatePicker();
        final DatePicker to = new DatePicker();

        DateRange(String label) {
            from.setLabel(label);
            from.setPlaceholder("from");
            to.setPlaceholder("until");
            from.setWidthFull();
            to.setWidthFull();
            setWidthFull();
            add(from, to);

            setAlignItems(Alignment.BASELINE);
        }

        void clear() {
            from.clear();
            to.clear();
        }
    }
}
