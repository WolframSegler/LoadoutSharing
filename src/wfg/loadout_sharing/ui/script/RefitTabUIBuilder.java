package wfg.loadout_sharing.ui.script;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.UIPanelAPI;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.util.IdentityMarker;
import wfg.utils.ui.CoreTabUIBuilder;

public final class RefitTabUIBuilder implements CoreTabUIBuilder {
    private static Object getRefitPanelMethod = null;

    private boolean uiInjected = false;

    @Override
    public boolean isDone() {
        return CoreTabUIBuilder.super.isDone() || uiInjected;
    }

    public final void advance(float delta) {
        if (!CoreUITabId.REFIT.equals(Global.getSector().getCampaignUI().getCurrentCoreTab())) return;
        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;

        final UIPanelAPI refitPanel = getRefitPanel(masterTab);
        if (refitPanel == null) return;

        if (IdentityMarker.isPresent(refitPanel)) return;
        IdentityMarker.attach(refitPanel);

        ButtonPairBuilder.injectButtons(refitPanel);

        uiInjected = true;
    }

    public static final UIPanelAPI getRefitPanel(final UIPanelAPI refitPanelParent) {
        if (getRefitPanelMethod == null) getRefitPanelMethod = RolfLectionUtil.getMethodDeclared("getRefitPanel", refitPanelParent.getClass());
        return (UIPanelAPI) RolfLectionUtil.invokeMethodDirectly(getRefitPanelMethod, refitPanelParent);
    }
}