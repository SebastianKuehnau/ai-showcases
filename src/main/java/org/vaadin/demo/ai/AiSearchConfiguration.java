package org.vaadin.demo.ai;

import com.vaadin.draiv.ai.mcp.CapabilityMcpTools;
import com.vaadin.draiv.ai.presenter.PresenterToolCatalog;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one piece of draiv-ai wiring this app still has to supply: the tool catalogue the text
 * presenter drives the UI with.
 * <p>
 * Everything else arrives by Spring Boot auto-configuration from {@code draiv} and
 * {@code draiv-ai}: the live-UI registry and the demo {@code DrivenUiResolver} (opted in with
 * {@code draiv.session.demo-identity=true} in {@code application.properties}), the
 * inspect/apply capability tool surface, provider selection, and the
 * {@code TextPresenterService} chat loop — which registers as soon as a bean named
 * {@code chatToolCatalog} exists.
 * <p>
 * The provider is built inline rather than exposed as a {@code ToolCallbackProvider} bean on
 * purpose: the MCP server auto-configuration collects every such bean and would publish these
 * tools to external agents as a side effect.
 */
@Configuration
public class AiSearchConfiguration {

    /** inspect / list_capabilities / apply / apply_many / navigate — nothing else. */
    @Bean(name = "chatToolCatalog")
    PresenterToolCatalog chatToolCatalog(CapabilityMcpTools capabilityTools) {
        return new PresenterToolCatalog(MethodToolCallbackProvider.builder()
                .toolObjects(capabilityTools)
                .build());
    }
}
