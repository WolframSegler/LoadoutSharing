package wfg.loadout_sharing.ui.script;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public interface CoreTabUIBuilder extends EveryFrameScript {
    default boolean isDone() { return !Global.getSector().isPaused(); }
    default boolean runWhilePaused() { return true; }
}