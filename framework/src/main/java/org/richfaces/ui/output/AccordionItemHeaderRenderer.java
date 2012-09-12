package org.richfaces.ui.output;

import static org.richfaces.ui.core.HtmlConstants.CLASS_ATTRIBUTE;
import static org.richfaces.ui.core.HtmlConstants.DIV_ELEM;
import static org.richfaces.ui.core.HtmlConstants.TD_ELEM;
import static org.richfaces.ui.output.DivPanelRenderer.attributeAsString;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.richfaces.ui.iteration.TableIconsRendererHelper;
import org.richfaces.ui.util.HtmlUtil;
import org.richfaces.ui.util.renderkit.PanelIcons;
import org.richfaces.ui.util.renderkit.PanelIcons.State;

class AccordionItemHeaderRenderer extends TableIconsRendererHelper<AbstractAccordionItem> {
    public AccordionItemHeaderRenderer() {
        super("header", "rf-ac-itm");
    }

    protected void encodeHeaderLeftIcon(ResponseWriter writer, FacesContext context, AbstractAccordionItem panel)
        throws IOException {
        String iconInactive = panel.isDisabled() ? panel.getLeftDisabledIcon() : panel.getLeftInactiveIcon();
        String iconActive = panel.isDisabled() ? panel.getLeftDisabledIcon() : panel.getLeftActiveIcon();

        encodeTdIcon(writer, context, cssClassPrefix + "-ico", iconInactive, iconActive,
            panel.isDisabled() ? State.headerDisabled : State.header);
    }

    protected void encodeHeaderRightIcon(ResponseWriter writer, FacesContext context, AbstractAccordionItem panel)
        throws IOException {
        String iconInactive = panel.isDisabled() ? panel.getRightDisabledIcon() : panel.getRightInactiveIcon();
        String iconActive = panel.isDisabled() ? panel.getRightDisabledIcon() : panel.getRightActiveIcon();

        // TODO nick - should this be "-ico-exp"? also why expanded icon state is connected with right icon alignment?
        encodeTdIcon(writer, context, cssClassPrefix + "-exp-ico", iconInactive, iconActive,
            panel.isDisabled() ? State.headerDisabled : State.header);
    }

    @Override
    protected void encodeTdIcon(ResponseWriter writer, FacesContext context, String cssClass, String attrIconCollapsedValue,
        String attrIconExpandedValue, PanelIcons.State state) throws IOException {
        if (isIconRendered(attrIconCollapsedValue) || isIconRendered(attrIconExpandedValue)) {
            writer.startElement(TD_ELEM, null);
            writer.writeAttribute(CLASS_ATTRIBUTE, cssClass, null);

            if (isIconRendered(attrIconExpandedValue)) {
                encodeIdIcon(writer, context, attrIconExpandedValue, cssIconsClassPrefix + "-act", state);
            }

            if (isIconRendered(attrIconCollapsedValue)) {
                encodeIdIcon(writer, context, attrIconCollapsedValue, cssIconsClassPrefix + "-inact", state);
            }

            writer.endElement(TD_ELEM);
        }
    }

    @Override
    protected void encodeHeaderTextValue(ResponseWriter writer, FacesContext context, AbstractAccordionItem titledItem)
        throws IOException {
        if (titledItem.isDisabled()) {
            encodeHeader(writer, context, titledItem, AbstractTogglePanelTitledItem.HeaderStates.disabled);
        } else {
            encodeHeader(writer, context, titledItem, AbstractTogglePanelTitledItem.HeaderStates.active);
            encodeHeader(writer, context, titledItem, AbstractTogglePanelTitledItem.HeaderStates.inactive);
        }
    }

    private static void encodeHeader(ResponseWriter writer, FacesContext context, AbstractAccordionItem component,
        AbstractTogglePanelTitledItem.HeaderStates state) throws IOException {
        writer.startElement(DIV_ELEM, null);
        writer.writeAttribute(CLASS_ATTRIBUTE,
            HtmlUtil.concatClasses("rf-ac-itm-lbl-" + state.abbreviation(), DivPanelRenderer.attributeAsString(component, state.headerClass())),
            null);

        writeFacetOrAttr(writer, context, component, "header", component.getHeaderFacet(state));

        writer.endElement(DIV_ELEM);
    }
}
