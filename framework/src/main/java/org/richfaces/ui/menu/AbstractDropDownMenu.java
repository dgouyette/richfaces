package org.richfaces.ui.menu;

import org.richfaces.cdk.annotations.Attribute;
import org.richfaces.cdk.annotations.Facet;
import org.richfaces.cdk.annotations.JsfComponent;
import org.richfaces.cdk.annotations.JsfRenderer;
import org.richfaces.cdk.annotations.Tag;

/**
 * The &lt;rich:dropDownMenu&gt; component is used for creating a drop-down, hierarchical menu. It can be used with the
 * &lt;rich:toolbar&gt; component to create menus in an application's toolbar.
 */
@JsfComponent(family = AbstractDropDownMenu.COMPONENT_FAMILY, type = AbstractDropDownMenu.COMPONENT_TYPE, facets = {
        @Facet(name = "label", generate = false), @Facet(name = "labelDisabled", generate = false) }, renderer = @JsfRenderer(type = DropDownMenuRendererBase.RENDERER_TYPE), tag = @Tag(name = "dropDownMenu"), attributes = {
        "events-mouse-props.xml", "events-key-props.xml", "core-props.xml", "i18n-props.xml", "position-props.xml" })
public abstract class AbstractDropDownMenu extends AbstractMenuContainer {
    public static final String COMPONENT_TYPE = "org.richfaces.DropDownMenu";
    public static final String COMPONENT_FAMILY = "org.richfaces.DropDownMenu";

    /**
     * The text label for the menu item. Alternatively, use the label facet to define content for the label
     */
    @Attribute
    public abstract String getLabel();

    public Object getCssRoot() {
        return "ddm";
    }

    public enum Facets {
        label,
        labelDisabled
    }
}
