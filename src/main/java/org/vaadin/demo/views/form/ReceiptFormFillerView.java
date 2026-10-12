package org.vaadin.demo.views.form;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.common.ValueSource;
import com.vaadin.flow.component.ai.form.FieldValueChangeEvent;
import com.vaadin.flow.component.ai.form.FormAIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.vaadin.demo.ai.LlmProviders;
import org.vaadin.demo.views.AiAssistantPanel;
import org.vaadin.demo.views.AiShowcaseLayout;
import org.vaadin.demo.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

/**
 * AI Form Filler from an image: an expense claim filled from a photographed receipt. The receipt
 * is sent to the LLM as an {@link AIAttachment}; {@link FormAIController} writes the extracted
 * values into the (unbound) fields.
 * <p>
 * Shows the source-tracking features: each AI-written field gets a marker whose popover lists the
 * confidence and the receipt text the value came from, and every change is reported through
 * {@link FormAIController#addFieldValueChangeListener}. Adapted from the Vaadin 25.3 feature tour.
 */
@Route(value = "form/receipt", layout = MainLayout.class)
@PageTitle("AI Form Filler – Receipt")
@Menu(order = 3, title = "AI Form Filler (Image)", icon = "vaadin:camera")
public class ReceiptFormFillerView extends AiShowcaseLayout {

    private static final String SYSTEM_PROMPT = """
            You help employees fill in expense claims. Use the currency code (e.g. EUR) for the \
            currency field. For the business purpose, write one short sentence. Leave fields empty \
            that the user or the document doesn't provide. Finally summarize in one sentence what \
            you filled.
            """;

    private final TextField merchant = new TextField("Merchant");
    private final DatePicker date = new DatePicker("Date of purchase");
    private final BigDecimalField total = new BigDecimalField("Total amount");
    private final ComboBox<String> currency = new ComboBox<>("Currency", "EUR", "USD", "GBP", "SEK", "CHF");
    private final Select<String> category = new Select<>();
    private final IntegerField guests = new IntegerField("Number of guests");
    private final ComboBox<String> payment = new ComboBox<>("Payment method", "Credit card", "Cash", "Bank transfer");
    private final TextArea purpose = new TextArea("Business purpose");
    private final Checkbox billable = new Checkbox("Billable to a customer");
    private final TextField costCenter = new TextField("Internal cost center");
    private final Div changeLog = new Div();

    public ReceiptFormFillerView(LlmProviders llmProviders) {
        this(new Div(), new FormLayout(), llmProviders);
    }

    private ReceiptFormFillerView(Div main, FormLayout form, LlmProviders llmProviders) {
        super(main, new AiAssistantPanel("AI Form Filler – Image",
                "Fill the expense claim from the sample receipt, or attach a photo of your own receipt.",
                llmProviders.isConfigured()));

        buildForm(form);
        byte[] receiptBytes = SampleReceipt.png();

        H3 changesTitle = new H3("Changes made by the AI");
        changeLog.addClassName("ai-change-log");
        changeLog.add(new Span("Each field the AI writes is listed here with its source confidence."));

        Div body = new Div(receiptPanel(receiptBytes), form);
        body.addClassName("receipt-showcase__body");
        main.addClassName("form-card");
        main.add(new H2("Expense claim"), body, changesTitle, changeLog);

        AiAssistantPanel panel = getPanel();
        if (llmProviders.isConfigured()) {
            UploadManager uploadManager = new UploadManager(this);
            UploadButton attach = new UploadButton(uploadManager);
            attach.setText("Attach receipt image");
            panel.addAboveInput(attach);

            FormAIController controller = new FormAIController(form)
                    .setSourceTrackingEnabled(true)
                    .ignoreField(costCenter)
                    .describeField(guests, "Number of people served, including the employee")
                    .describeField(billable,
                            "True when the expense relates to a customer project or customer meeting")
                    .setFieldMarkerPopoverContentProvider(ReceiptFormFillerView::sourceDetails);
            controller.addFieldValueChangeListener(this::logChange);

            AIOrchestrator orchestrator = AIOrchestrator.builder(llmProviders.create(), SYSTEM_PROMPT)
                    .withMessageList(panel.getMessageList())
                    .withInput(panel.getInput())
                    .withFileReceiver(uploadManager)
                    .withController(controller)
                    .withAssistantName("Expense assistant")
                    .build();

            Button fromReceipt = new Button("Fill from the sample receipt", VaadinIcon.MAGIC.create(),
                    _ -> orchestrator.prompt("Fill in the expense claim from the attached receipt.",
                            List.of(new AIAttachment("receipt.png", "image/png", receiptBytes))));
            fromReceipt.addThemeVariants(ButtonVariant.PRIMARY);
            fromReceipt.setWidthFull();
            panel.addComponentAtIndex(2, fromReceipt); // below title and hint
            panel.setExamples(orchestrator::prompt,
                    "Taxi from the airport to the hotel yesterday, 38 euros, paid by card. "
                            + "It was for the Vaadin Create conference.",
                    "Clear the guests and the business purpose");
        }
    }

    private void buildForm(FormLayout form) {
        // Stable ids keep the LLM's tool calls and logs readable
        merchant.setId("merchant");
        date.setId("date");
        total.setId("total");
        currency.setId("currency");
        category.setId("category");
        category.setLabel("Category");
        category.setItems("Meals & entertainment", "Travel", "Accommodation", "Office supplies", "Software");
        guests.setId("guests");
        payment.setId("payment");
        purpose.setId("purpose");
        billable.setId("billable");
        costCenter.setId("cost-center");
        costCenter.setValue("CC-4711");
        costCenter.setHelperText("Hidden from the AI with ignoreField()");
        date.setRequiredIndicatorVisible(true);
        total.setRequiredIndicatorVisible(true);

        form.setAutoResponsive(true);
        form.setExpandFields(true);
        form.addFormRow(merchant, date);
        form.addFormRow(total, currency);
        form.addFormRow(category, guests);
        form.addFormRow(payment, billable);
        form.addFormRow(purpose);
        form.setColspan(purpose, 2);
        form.addFormRow(costCenter);
    }

    private static Div receiptPanel(byte[] receiptBytes) {
        Image preview = new Image(receiptDownload(receiptBytes), "Sample receipt");
        preview.addClassName("receipt-preview");
        Anchor download = new Anchor(receiptDownload(receiptBytes), "Download receipt.png");
        download.getElement().setAttribute("download", "receipt.png");

        Div panel = new Div(preview, download);
        panel.addClassName("receipt-panel");
        return panel;
    }

    private static DownloadHandler receiptDownload(byte[] receiptBytes) {
        return DownloadHandler.fromInputStream(_ -> new DownloadResponse(
                new ByteArrayInputStream(receiptBytes), "receipt.png", "image/png", receiptBytes.length));
    }

    /** Popover content of an AI field marker: confidence plus the quoted source text. */
    private static Component sourceDetails(FieldValueChangeEvent change) {
        return change.getFieldSource().map(ReceiptFormFillerView::sourceDetails).orElse(null);
    }

    private static Component sourceDetails(ValueSource source) {
        Div details = new Div();
        details.addClassName("ai-source");
        if (source.confidence() != null) {
            Span confidence = new Span("Confidence: " + source.confidence().name().toLowerCase(Locale.ROOT));
            confidence.addClassNames("ai-confidence", switch (source.confidence()) {
                case HIGH -> "high";
                case MEDIUM -> "medium";
                default -> "low";
            });
            details.add(confidence);
        }
        source.extracts().forEach(extract -> {
            Span quote = new Span("“" + extract.text() + "”");
            quote.addClassName("ai-extract");
            details.add(quote);
        });
        return details;
    }

    private void logChange(FieldValueChangeEvent event) {
        if (changeLog.getComponentCount() == 1 && changeLog.getComponentAt(0) instanceof Span) {
            changeLog.removeAll();
        }
        String label = event.getField() instanceof HasLabel hasLabel ? hasLabel.getLabel() : "Field";
        String confidence = event.getFieldSource().map(ValueSource::confidence)
                .map(level -> " · confidence " + level.name().toLowerCase(Locale.ROOT)).orElse("");
        Div entry = new Div(new Span(label + ": "),
                new Span(format(event.getOldValue()) + " → " + format(event.getNewValue())),
                new Span(confidence));
        entry.addClassName("ai-change");
        changeLog.addComponentAsFirst(entry);
    }

    private static String format(Object value) {
        return value == null || "".equals(value) ? "(empty)" : String.valueOf(value);
    }
}
