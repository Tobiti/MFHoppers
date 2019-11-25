package net.squidstudios.mfhoppers.manager;

import net.squidstudios.mfhoppers.MFHoppers;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class HookManager {

    private boolean isWildStackerHooked;

    private static HookManager instance;
    public HookManager(MFHoppers hoppers){

        isWildStackerHooked = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
        instance = this;

    }

    public boolean isWildStackerHooked() {
        return isWildStackerHooked;
    }

    public static HookManager getInstance() {
        return instance;
    }
}
