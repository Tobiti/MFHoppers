package net.squidstudios.mfhoppers.manager;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.hopper.UnloadedHopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldManager implements Listener {

    private DataManager manager;
    private static WorldManager instance;
    private Map<String, List<UnloadedHopper>> needsToBeLoaded = new HashMap<>();

    public WorldManager(DataManager manager){
        this.manager = manager;
        instance = this;
    }

    public void add(UnloadedHopper hopper){

        List<UnloadedHopper> hoppers = new ArrayList<>();
        String worldName = hopper.getWorldName();
        if(needsToBeLoaded.containsKey(worldName)) hoppers = needsToBeLoaded.get(worldName);
        hoppers.add(hopper);
        needsToBeLoaded.remove(worldName);
        needsToBeLoaded.put(worldName,hoppers);

    }

    public void remove(UnloadedHopper hopper){

        List<UnloadedHopper> hoppers = new ArrayList<>();
        String worldName = hopper.getWorldName();
        if(needsToBeLoaded.containsKey(worldName)) hoppers = needsToBeLoaded.get(worldName);
        hoppers.remove(hopper);
        needsToBeLoaded.remove(worldName);
        needsToBeLoaded.put(worldName,hoppers);

    }

    @EventHandler
    public void onLoad(WorldLoadEvent event){

        if(needsToBeLoaded.containsKey(event.getWorld().getName())){

            new BukkitRunnable(){
                @Override
                public void run() {

                    for(UnloadedHopper hopper : needsToBeLoaded.get(event.getWorld().getName())){

                        manager.add(hopper.build(), false);

                    }

                }
            }.runTaskLater(MFHoppers.getInstance(), 4);

        }

    }

    public static WorldManager getInstance() {
        return instance;
    }
}
