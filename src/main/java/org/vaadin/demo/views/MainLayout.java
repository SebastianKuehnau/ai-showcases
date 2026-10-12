package org.vaadin.demo.views;

import com.vaadin.draiv.ai.ui.UiAssistant;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import org.vaadin.demo.views.navigation.AiNavigationBar;

/**
 * Application shell: a drawer whose navigation is built from the views' {@code @Menu} annotations,
 * and the app-wide {@link AiNavigationBar} in the navbar. The layout survives navigation, so the
 * bar keeps its state while the AI moves between views.
 */
public class MainLayout extends AppLayout {

    public MainLayout(UiAssistant assistant) {
        Span title = new Span("Vaadin AI Showcase");
        title.addClassName("app-title");
        addToNavbar(new DrawerToggle(), title, new AiNavigationBar(assistant));

        SideNav nav = new SideNav();
        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(entry.icon() == null
                ? new SideNavItem(entry.title(), entry.path())
                : new SideNavItem(entry.title(), entry.path(), new Icon(entry.icon()))));
        addToDrawer(nav);
    }
}
