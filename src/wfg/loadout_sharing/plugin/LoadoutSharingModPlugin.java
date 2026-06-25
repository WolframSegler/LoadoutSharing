package wfg.loadout_sharing.plugin;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import wfg.loadout_sharing.ui.script.UIInjectorListener;

public class LoadoutSharingModPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().getListenerManager().addListener(new UIInjectorListener(), true);
    }
}