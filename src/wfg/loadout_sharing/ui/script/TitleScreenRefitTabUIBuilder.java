package wfg.loadout_sharing.ui.script;

import static wfg.native_ui.util.Globals.settings;

import java.util.List;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.MethodFields;
import wfg.native_ui.ui.util.IdentityMarker;

public final class TitleScreenRefitTabUIBuilder extends BaseEveryFrameCombatPlugin {
    private boolean inTitleScreen;

    @Override
    public void init(CombatEngineAPI engine) {
        inTitleScreen = settings.getCurrentState() == GameState.TITLE;
    }

    @Override
    public void advance(float delta, List<InputEventAPI> events) {
        if (!inTitleScreen) return;

        final UIPanelAPI screen = Attachments.getTitleScreenPanel();

        UIPanelAPI targetOuter2 = null;
        for (final UIComponentAPI child : MethodFields.getChildrenNonCopy(screen)) {
            if (!isTargetSize(child, 1024f, 768f)) continue;

            if (!RolfLectionUtil.hasMethodOfName("isSuspendEventInterception", child)) continue;

            targetOuter2 = (UIPanelAPI) child;
            break;
        }
        if (targetOuter2 == null) return;

        final UIPanelAPI targetOuter = (UIPanelAPI) MethodFields.getChildrenNonCopy(targetOuter2).get(0);

        UIComponentAPI target = null;
        for (final UIComponentAPI child : MethodFields.getChildrenNonCopy(targetOuter)) {
            if (!isTargetSize(child, 1024f, 748f)) continue;

            target = child;
            break;
        }

        final UIComponentAPI dialogContent = (UIComponentAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getCurr", target);
        if (dialogContent == null) return;

        final UIPanelAPI refitPanel = RefitTabUIBuilder.getRefitPanel((UIPanelAPI) dialogContent);

        if (IdentityMarker.isPresent(refitPanel)) return;
        IdentityMarker.attach(refitPanel);

        ButtonPairBuilder.injectButtons(refitPanel);
    }

    private static final boolean isTargetSize(final UIComponentAPI comp, float w, float h) {
        return Math.abs(comp.getPosition().getWidth() - w) <= 1f
            && Math.abs(comp.getPosition().getHeight() - h) <= 1f;
    }
}