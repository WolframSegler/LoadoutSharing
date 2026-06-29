
package wfg.loadout_sharing.ui.script;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.loading.specs.HullVariantSpec;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.component.InteractionComp.ShortcutHandler;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class RefitTabUIBuilder implements CoreTabUIBuilder {
    private static final int BTN_W = 50;
    private static final int BTN_H = 12;

    private static Object getRefitPanelMethod = null;
    private static Object getMemberMethod = null;
    private static Object syncWithCurrVariantMethod = null;
    private static Object recreateUIMethod = null;
    private static Object getShipDisplayMethod = null;
    private static Object getCurrentVariantMethod = null;

    private boolean uiInjected = false;

    @Override
    public boolean isDone() {
        return CoreTabUIBuilder.super.isDone() || uiInjected;
    }

    public final void advance(float delta) {
        if (!CoreUITabId.REFIT.equals(Global.getSector().getCampaignUI().getCurrentCoreTab())) return;
        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;

        if (getRefitPanelMethod == null) getRefitPanelMethod = RolfLectionUtil.getMethodDeclared("getRefitPanel", masterTab.getClass());
        final UIPanelAPI refitPanel = (UIPanelAPI) RolfLectionUtil.invokeMethodDirectly(getRefitPanelMethod, masterTab);
        if (refitPanel == null) return;
        if (getMemberMethod == null) getMemberMethod = RolfLectionUtil.getMethodDeclared("getMember", refitPanel.getClass());
        if (syncWithCurrVariantMethod == null) syncWithCurrVariantMethod = RolfLectionUtil.getMethodDeclared("syncWithCurrentVariant", refitPanel.getClass(), 1);
        if (recreateUIMethod == null) recreateUIMethod = RolfLectionUtil.getMethodDeclared("recreateUI", refitPanel.getClass());

        if (IdentityMarker.isPresent(refitPanel)) return;
        IdentityMarker.attach(refitPanel);

        injectButtons(refitPanel);

        uiInjected = true;
    }

    private static final void injectButtons(final UIPanelAPI parent) {
        final Button copyBtn = new Button(parent, BTN_W, BTN_H, "copy", Fonts.VICTOR_10, (btn) -> {
            final HullVariantSpec spec = (HullVariantSpec) getActiveVariant(parent);
            String variantJson = null;
            try {
                variantJson = spec.toJSONObject().toString();
            } catch (Exception e) {} finally {
                if (variantJson == null) variantJson = "failed to copy variant";
            }
            copyToClipboard(variantJson);
        }) {{
            final ShortcutHandler<Button> OnShortcut = interaction.onShortcutPressed;
            interaction.onShortcutPressed = (btn, event) -> {
                if (NativeUiUtils.isCtrlDown() || event == null) OnShortcut.run(btn, event);
            };
        }};

        final Button pasteBtn = new Button(parent, BTN_W, BTN_H, "paste", Fonts.VICTOR_10, (btn) -> {
            final ShipVariantAPI orgSpec = getActiveVariant(parent);
            final ShipVariantAPI spec;
            try {
                final JSONObject data = new JSONObject(getClipboardText());
                spec = new HullVariantSpec(data);
            } catch (Exception e) {
                Global.getLogger(RefitTabUIBuilder.class).warn("failed to paste variant");
                return;
            }

            if (!spec.getHullSpec().getHullId().equals(orgSpec.getHullSpec().getHullId())) return;

            spawVariants(getActiveVariant(parent), spec);
            RolfLectionUtil.invokeMethodDirectly(syncWithCurrVariantMethod, parent, true);
        }) {{
            final ShortcutHandler<Button> OnShortcut = interaction.onShortcutPressed;
            interaction.onShortcutPressed = (btn, event) -> {
                if (NativeUiUtils.isCtrlDown() || event == null) OnShortcut.run(btn, event);
            };
        }};

        copyBtn.setShortcut(Keyboard.KEY_C);
        pasteBtn.setShortcut(Keyboard.KEY_V);
        copyBtn.cutStyle = CutStyle.TL_BR;
        pasteBtn.cutStyle = CutStyle.TL_BR;
        parent.addComponent(copyBtn.getPanel()).inBR(pad, pad);
        parent.addComponent(pasteBtn.getPanel()).leftOfMid(copyBtn.getPanel(), pad);

        copyBtn.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, copyBtn.getPanel(), AnchorType.TopRight, hpad);
        pasteBtn.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, pasteBtn.getPanel(), AnchorType.TopRight, hpad);
        copyBtn.tooltip.builder = (tp, expanded) -> {
            tp.addPara("Copy the current variant to the clipboard [Ctrl-C]", pad, highlight, "Ctrl-C");
        };
        pasteBtn.tooltip.builder = (tp, expanded) -> {
            tp.addPara("Paste the current variant from the clipboard [Ctrl-V]", pad, highlight, "Ctrl-V");
        };
    }

    private static final void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    private static final String getClipboardText() {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Transferable transferable = clipboard.getContents(null);
        final boolean validClipboard = transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor);

        String string = "";
        if (validClipboard) {
            try {
                string = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                Global.getLogger(RefitTabUIBuilder.class).warn("illegal clipboard content");
            }
        }
        return string;
    }

    private static final UIPanelAPI getShipDisplay(UIPanelAPI refitPanel) {
        if (getShipDisplayMethod == null) getShipDisplayMethod = RolfLectionUtil.getMethodDeclared("getShipDisplay", refitPanel.getClass());
        return (UIPanelAPI) RolfLectionUtil.invokeMethodDirectly(getShipDisplayMethod, refitPanel);
    }

    private static final ShipVariantAPI getActiveVariant(UIPanelAPI refitPanel) {
        final UIPanelAPI shipDisplay = getShipDisplay(refitPanel);

        if (getCurrentVariantMethod == null) getCurrentVariantMethod = RolfLectionUtil.getMethodDeclared("getCurrentVariant", shipDisplay.getClass());
        return (ShipVariantAPI) RolfLectionUtil.invokeMethodDirectly(getCurrentVariantMethod, shipDisplay);
    }

    private static final void spawVariants(ShipVariantAPI oldVariant, ShipVariantAPI newVariant) {
        final CharacterDataAPI characterData = Global.getSector().getCharacterData();
        final MarketAPI openMarket = Global.getSector().getCurrentlyOpenMarket();
        final CargoAPI planetCargo = openMarket == null ? null : openMarket.hasSubmarket(Submarkets.SUBMARKET_STORAGE)
            ? openMarket.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo() : null;
        final CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        final boolean hasStorage = planetCargo != null;

        oldVariant.setSource(VariantSource.REFIT);

        for (String slotId : oldVariant.getNonBuiltInWeaponSlots()) {
            final String weaponId = oldVariant.getWeaponId(slotId);
            if (weaponId != null) playerCargo.addWeapons(weaponId, 1);
        }

        for (String wingId : oldVariant.getNonBuiltInWings()) {
            playerCargo.addFighters(wingId, 1);
        }
        oldVariant.clear();

        for (String slotId : newVariant.getNonBuiltInWeaponSlots()) {
            final String weaponId = newVariant.getWeaponId(slotId);
            final boolean playerHasWeapon = playerCargo.getNumWeapons(weaponId) > 0;
            final boolean storageHasWeapon = hasStorage && planetCargo.getNumWeapons(weaponId) > 0;
            if (!playerHasWeapon && !storageHasWeapon) continue;
            if (playerHasWeapon) playerCargo.removeWeapons(weaponId, 1);
            else planetCargo.removeWeapons(weaponId, 1);
            oldVariant.addWeapon(slotId, weaponId);
        }
        
        final List<String> wings = newVariant.getWings();
        for (int i = 0; i < wings.size(); i++) {
            if (oldVariant.getHullSpec().isBuiltInWing(i)) continue;
            final String wingId = wings.get(i);
            final boolean playerHasWing = playerCargo.getNumFighters(wingId) > 0;
            final boolean storageHasWing = hasStorage && planetCargo.getNumFighters(wingId) > 0;
            if (!playerHasWing && !storageHasWing) continue;
            if (playerHasWing) playerCargo.removeFighters(wingId, 1);
            else planetCargo.removeFighters(wingId, 1);
            oldVariant.setWingId(i, wingId);
        }

        for (String modId : newVariant.getNonBuiltInHullmods()) {
            if (oldVariant.hasHullMod(modId) || !characterData.knowsHullMod(modId)) continue;
            oldVariant.addMod(modId);
        }

        oldVariant.setNumFluxCapacitors(newVariant.getNumFluxCapacitors());
        oldVariant.setNumFluxVents(newVariant.getNumFluxVents());
    }
}