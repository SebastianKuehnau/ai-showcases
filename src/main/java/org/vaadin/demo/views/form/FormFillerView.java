package org.vaadin.demo.views.form;

import com.vaadin.flow.component.ai.form.FormAIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.demo.ai.LlmProviders;
import org.vaadin.demo.data.Address;
import org.vaadin.demo.data.Customer;
import org.vaadin.demo.data.CustomerRepository;
import org.vaadin.demo.views.AiAssistantPanel;
import org.vaadin.demo.views.AiShowcaseLayout;
import org.vaadin.demo.views.MainLayout;

import java.time.LocalDate;
import java.util.List;

/**
 * AI Form Filler: a "new customer" form backed by a {@link Binder}. {@link FormAIController}
 * lets the LLM read the form, fill it from free text or an attached file (e-mail signature,
 * business card photo, PDF), and validates every write through the binder. Changed fields get an
 * "AI" marker the user can revert. Saving stays a deliberate user action.
 */
@Route(value = "form", layout = MainLayout.class)
@PageTitle("AI Form Filler")
@Menu(order = 2, title = "AI Form Filler (Text)", icon = "vaadin:form")
public class FormFillerView extends AiShowcaseLayout {

    private static final String SYSTEM_PROMPT = """
            You help sales staff capture new customer records. Fill only fields the user's text or \
            attachment supports; never invent e-mail addresses, phone numbers or scores. Derive the \
            ISO country code from the country. Phone numbers use international format (+49…). \
            After filling, list in one or two sentences what you filled and what is still missing.
            """;

    private final TextField companyName = new TextField("Company name");
    private final TextField contactName = new TextField("Contact name");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    private final Checkbox active = new Checkbox("Active customer");
    private final BigDecimalField annualRevenue = new BigDecimalField("Annual revenue (€)");
    private final IntegerField creditScore = new IntegerField("Credit score");
    private final DatePicker customerSince = new DatePicker("Customer since");
    private final TextField street = new TextField("Street");
    private final TextField houseNumber = new TextField("House number");
    private final TextField postalCode = new TextField("Postal code");
    private final TextField city = new TextField("City");
    private final TextField state = new TextField("State / region");
    private final ComboBox<String> country = new ComboBox<>("Country");
    private final TextField countryCode = new TextField("Country code");

    private final Binder<Customer> binder = new Binder<>(Customer.class);
    private final CustomerRepository customerRepository;

    public FormFillerView(LlmProviders llmProviders, CustomerRepository customerRepository) {
        this(new Div(), new FormLayout(), llmProviders, customerRepository);
    }

    private FormFillerView(Div main, FormLayout form, LlmProviders llmProviders,
                           CustomerRepository customerRepository) {
        super(main, new AiAssistantPanel("AI Form Filler",
                "Paste an e-mail signature, describe the customer or attach a business card / PDF.",
                llmProviders.isConfigured()));
        this.customerRepository = customerRepository;

        buildForm(form);
        bindFields();
        newCustomer();

        Button save = new Button("Save customer", _ -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button clear = new Button("Clear", _ -> newCustomer());
        main.addClassName("form-card");
        main.add(new H2("New customer"), form, new HorizontalLayout(save, clear));

        AiAssistantPanel panel = getPanel();
        if (llmProviders.isConfigured()) {
            UploadManager uploadManager = new UploadManager(this);
            UploadButton attach = new UploadButton(uploadManager);
            attach.setText("Attach file");
            panel.addAboveInput(attach);

            FormAIController controller = new FormAIController(form, binder)
                    .describeField(creditScore, "Normalized credit score from 0 (worst) to 100 (best). "
                            + "Only fill when a score or rating is given: good ≈ 80, limited ≈ 55, at risk ≈ 25.")
                    .describeField(annualRevenue, "The customer company's own annual revenue in EUR.")
                    .describeField(countryCode, "ISO 3166-1 alpha-2 code matching the country, e.g. DE.");

            AIOrchestrator orchestrator = AIOrchestrator.builder(llmProviders.create(), SYSTEM_PROMPT)
                    .withMessageList(panel.getMessageList())
                    .withInput(panel.getInput())
                    .withFileReceiver(uploadManager)
                    .withController(controller)
                    .build();
            panel.setExamples(orchestrator::prompt,
                    """
                    Dr. Sabine Krüger | Head of Procurement
                    Nordlicht Logistik GmbH
                    Speicherstadt 12, 20457 Hamburg
                    T +49 40 123456-0 | s.krueger@nordlicht-logistik.example""",
                    "New active customer since today: Bella Napoli Foods, Milano, Italy, contact Marco Rossi, "
                            + "good credit, about 2.4 million revenue",
                    "Clear the address fields");
        }
    }

    private void buildForm(FormLayout form) {
        country.setItems(List.of("Austria", "Belgium", "Denmark", "France", "Germany", "Italy",
                "Netherlands", "Poland", "Spain", "Sweden", "Switzerland", "United Kingdom", "United States"));
        country.setAllowCustomValue(true);
        country.addCustomValueSetListener(e -> country.setValue(e.getDetail()));
        creditScore.setMin(0);
        creditScore.setMax(100);
        creditScore.setHelperText("0–100, ≥ 70 is creditworthy");
        customerSince.setMax(LocalDate.now());

        form.setAutoResponsive(true);
        form.setExpandFields(true);
        form.addFormRow(companyName, contactName);
        form.addFormRow(email, phone);
        form.addFormRow(annualRevenue, creditScore, customerSince);
        form.addFormRow(active);
        form.addFormRow(street, houseNumber);
        form.addFormRow(postalCode, city, state);
        form.addFormRow(country, countryCode);
    }

    private void bindFields() {
        binder.forField(companyName).asRequired("Company name is required").bind("companyName");
        binder.forField(contactName).bind("contactName");
        binder.forField(email).withValidator(new EmailValidator("Not a valid e-mail address", true)).bind("email");
        binder.forField(phone).bind("phone");
        binder.forField(active).bind("active");
        binder.forField(annualRevenue).bind("annualRevenue");
        binder.forField(creditScore)
                // creditScore is a primitive int: show "not set" (0) as an empty field
                .withConverter(score -> score == null ? 0 : score, score -> score == 0 ? null : score)
                .withValidator(new IntegerRangeValidator("Must be between 0 and 100", 0, 100))
                .bind("creditScore");
        binder.forField(customerSince).bind("customerSince");
        binder.forField(street).bind("address.street");
        binder.forField(houseNumber).bind("address.houseNumber");
        binder.forField(postalCode).bind("address.postalCode");
        binder.forField(city).asRequired("City is required").bind("address.city");
        binder.forField(state).bind("address.state");
        binder.forField(country).asRequired("Country is required").bind("address.country");
        binder.forField(countryCode).bind("address.countryCode");
    }

    private void newCustomer() {
        Customer customer = new Customer();
        customer.setAddress(new Address());
        customer.setActive(true);
        binder.setBean(customer);
    }

    private void save() {
        if (binder.validate().isOk()) {
            Customer saved = customerRepository.save(binder.getBean());
            Notification.show("Saved " + saved.getCompanyName() + " (ID " + saved.getId() + ")", 3000,
                    Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.SUCCESS);
            newCustomer();
        }
    }
}
