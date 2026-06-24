package wfg.loadout_sharing.plugin;

import static wfg.loadout_sharing.constant.Mods.LUNA_LIB;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;
import wfg.loadout_sharing.ui.script.UIInjectorListener;

public class LoadoutSharingModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        if (settings.getModManager().isModEnabled(LUNA_LIB)) {
            LunaSettings.addSettingsListener(null); // TODO handle
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().getListenerManager().addListener(new UIInjectorListener(), true);
    }

    @Override
    public void beforeGameSave() {
    }

    @Override
    public void afterGameSave() {
    }
}