package wfg.loadout_sharing.ui.script;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;

public class UIInjectorListener implements CoreUITabListener {
    
    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tabID, Object param) {
        final SectorAPI sector = Global.getSector();

        sector.removeTransientScriptsOfClass(CoreTabUIBuilder.class);

        switch (tabID) {
        case REFIT:
            sector.addTransientScript(new RefitTabUIBuilder());
            break;
    
        default: break;
        }
    }
}