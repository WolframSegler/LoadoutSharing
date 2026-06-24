
package wfg.loadout_sharing.ui.script;

import static wfg.native_ui.util.UIConstants.pad;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class RefitTabUIBuilder implements CoreTabUIBuilder {

    @SuppressWarnings("unchecked")
    public void advance(float delta) {
        final MarketAPI market = Global.getSector().getCurrentlyOpenMarket();
        if (market == null) return;

        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;

        final HashMap<String, ItemMarker> activeMarkers = ItemMarkersMap.instance().activeMarkers;
        final UIPanelAPI marketPicker = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getMarketPicker", masterTab);

        if (marketPicker != null && !IdentityMarker.isPresent(marketPicker)) {
            IdentityMarker.attach(marketPicker);
    
            final List<ButtonAPI> submarketButtons = (List<ButtonAPI>) RolfLectionUtil.getAllVariables(marketPicker).stream()
                .filter(f -> f instanceof List).findFirst().orElse(null);
    
            for (ButtonAPI btn : submarketButtons) {
                final SubmarketAPI submarket = ((SubmarketAPI)btn.getCustomData());
    
                submarket.getCargo().initMothballedShips(submarket.getFaction().getId());
                for (FleetMemberAPI member : submarket.getCargo().getMothballedShips().getMembersListCopy()) {
                    if (activeMarkers.containsKey(member.getHullSpec().getBaseHullId())) {
                        final Base icon = new Base(marketPicker, 20, 20, Sprites.MARKER, null, null);
                        marketPicker.addComponent(icon.getPanel()).rightOfTop(btn, -16);
                        break;
                    }
                }
            }
        }

        final SubmarketAPI submarket = (SubmarketAPI) RolfLectionUtil.getAllVariables(masterTab).stream()
            .filter(f -> f instanceof SubmarketAPI).findFirst().orElse(null);
        if (submarket == null) return;
        final String buyVerb = submarket.getPlugin().getBuyVerb();

        boolean isInBuyMode = false;
        for (Object panel : (List<Object>) RolfLectionUtil.invokeMethodDirectly(CustomPanel.getChildrenNonCopyMethod, masterTab)) {
            if (panel instanceof ButtonAPI button && button.getText().startsWith(buyVerb) && button.isHighlighted()) {
                isInBuyMode = true;
                break;
            }
        }
        if (!isInBuyMode) return;

    
        final UIPanelAPI fleetPanel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getFleetPanel", masterTab);
        if (fleetPanel == null) return;

        final UIPanelAPI fleetList = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getList", fleetPanel);
        final List<UIComponentAPI> widgets = (List<UIComponentAPI>) RolfLectionUtil.getMethodAndInvokeDirectly("getItems", fleetList);

        for (UIComponentAPI widgetObj : widgets) {
            final UIPanelAPI widget = (UIPanelAPI) widgetObj;

            if (IdentityMarker.isPresent(widget)) continue;
            IdentityMarker.attach(widget);

            final FleetMemberAPI member = (FleetMember) RolfLectionUtil.getMethodAndInvokeDirectly("getMember", widget);

            if (!activeMarkers.containsKey(member.getHullSpec().getBaseHullId())) continue;
            final Base icon = new Base(widget, 20, 20, Sprites.MARKER, null, null);
            widget.addComponent(icon.getPanel()).inBL(pad, 70);

            if (VisualConfig.HIGHLIGHT_FRAME_ALPHA > 0f) {
                final Base hue = new Base(widget, (int) widget.getPosition().getWidth(), (int) widget.getPosition().getHeight(), Sprites.HUE_FRAME, null, null);
                widget.addComponent(hue.getPanel()).inBL(0f, 0f);
                hue.texColor = new Color(1f, 1f, 1f, VisualConfig.HIGHLIGHT_FRAME_ALPHA);
            }
        }
    }
}