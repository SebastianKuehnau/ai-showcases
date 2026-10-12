package org.vaadin.demo.views.customers;

import com.vaadin.draiv.ai.ui.AiGoalBar;
import com.vaadin.draiv.ai.ui.UiAssistant;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.demo.views.MainLayout;
import org.vaadin.demo.data.Customer;
import org.vaadin.demo.data.CustomerFilter;
import org.vaadin.demo.data.CustomerRepository;
import org.vaadin.demo.data.CustomerSpecifications;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Customers – AI Search")
@Menu(order = 1, title = "Customers (draiv)", icon = "vaadin:users")
public class CustomerListView extends VerticalLayout {

    private static final NumberFormat REVENUE_FORMAT = NumberFormat.getNumberInstance(Locale.GERMANY);

    /** How the AI should operate THIS screen: fill the query-builder form, then click Search. */
    private static final String AI_GOAL = """
            The user described a customer search in plain language. Using the customer search \
            form currently on screen, set only the filter fields this request implies (leave \
            every other field untouched), then click the Search button to run it.

            Map the request to the form sensibly:
            - company / contact / email / city: the matching text fields;
            - country: the Country multi-select;
            - active vs. inactive: the Status field;
            - credit standing: the Credit rating field (Creditworthy = good, Limited = medium, \
            At risk = poor);
            - "revenue over/under/between …": the Annual revenue min/max fields;
            - time phrases about when they became a customer or last ordered: the Customer since \
            / Last order from–until date fields (today's date is given above).

            Do not invent constraints the user did not ask for. Request: "%s"
            """;

    private final Grid<Customer> grid;
    private final CustomerRepository customerRepository;

    public CustomerListView(CustomerRepository customerRepository, UiAssistant assistant) {
        this.customerRepository = customerRepository;
        add(new H1("Customer Grid – Query Builder"));

        CustomerSearchForm searchForm = new CustomerSearchForm(distinctCountries(), this::applyFilter);
        // AiGoalBar turns on change indication (FLASH by default) so the fields the AI fills glow —
        // no wiring needed here.
        AiGoalBar aiBar = new AiGoalBar(assistant, "Search", AI_GOAL);
        aiBar.setPlaceholder("Describe your search, e.g. \"active German customers with good "
                + "credit and revenue over 100k\"");
        add(aiBar);
        add(searchForm);

        grid = new Grid<>(Customer.class);
        grid.setColumns("companyName", "contactName", "email", "phone", "customerSince", "lastOrderDate");
        grid.addColumn(customer -> customer.getAnnualRevenue() == null ?
                        "" : REVENUE_FORMAT.format(customer.getAnnualRevenue()) + " €")
                .setHeader("Annual Revenue").setKey("annualRevenue").setSortable(true)
                .setComparator(Comparator.comparing(Customer::getAnnualRevenue, Comparator.nullsFirst(Comparator.naturalOrder())))
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(Customer::getAddress).setHeader("Address").setKey("address").setSortable(true).setComparator(CustomerListView::compareAddress).setFlexGrow(2);
        grid.addComponentColumn(CreditScoreIndicator::new).setHeader("Credit Rating").setKey("creditRating")
                .setComparator(Comparator.comparingInt(Customer::getCreditScore));
        grid.setItems(customerRepository.findAll());
        grid.setSizeFull();
        add(grid);

        setSizeFull();
    }

    /** Viewport width (px) at or above which the medium-priority columns are shown. */
    private static final int MEDIUM_BREAKPOINT = 768;
    /** Viewport width (px) at or above which the large-priority columns are shown. */
    private static final int LARGE_BREAKPOINT = 1200;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        var page = attachEvent.getUI().getPage();
        // Apply once for the initial window size, then on every resize.
        page.retrieveExtendedClientDetails(details -> applyResponsiveColumns(details.getWindowInnerWidth()));
        page.addBrowserWindowResizeListener(event -> applyResponsiveColumns(event.getWidth()));
    }

    /**
     * Shows or hides columns by priority based on the viewport width:
     * <ul>
     *     <li>always: Company Name, Contact Name, Credit Rating</li>
     *     <li>medium (≥ {@value #MEDIUM_BREAKPOINT}px): + Address, Phone, Email</li>
     *     <li>large (≥ {@value #LARGE_BREAKPOINT}px): + Customer Since, Last Order Date, Annual Revenue</li>
     * </ul>
     */
    private void applyResponsiveColumns(int width) {
        boolean medium = width >= MEDIUM_BREAKPOINT;
        boolean large = width >= LARGE_BREAKPOINT;

        grid.getColumnByKey("address").setVisible(medium);
        grid.getColumnByKey("phone").setVisible(medium);
        grid.getColumnByKey("email").setVisible(medium);

        grid.getColumnByKey("customerSince").setVisible(large);
        grid.getColumnByKey("lastOrderDate").setVisible(large);
        grid.getColumnByKey("annualRevenue").setVisible(large);
    }

    private void applyFilter(CustomerFilter filter) {
        grid.setItems(customerRepository.findAll(CustomerSpecifications.matching(filter)));
    }

    /** Distinct country names present in the data, to populate the country selector. */
    private List<String> distinctCountries() {
        return customerRepository.findAll().stream()
                .map(customer -> customer.getAddress().getCountry())
                .distinct()
                .sorted()
                .toList();
    }

    private static int compareAddress(Customer c1, Customer c2) {
        var a1 = c1.getAddress();
        var a2 = c2.getAddress();

        return a1.getCountry().compareTo(a2.getCountry()) != 0 ?
                a1.getCountry().compareTo(a2.getCountry()) : a1.getCountry().compareTo(a2.getCountry()) != 0 ?
                a1.getCity().compareTo(a2.getCity()) : a1.getPostalCode().compareTo(a2.getPostalCode());
    }
}
