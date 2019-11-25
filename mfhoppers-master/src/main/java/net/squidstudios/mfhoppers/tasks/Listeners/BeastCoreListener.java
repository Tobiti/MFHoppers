package net.squidstudios.mfhoppers.tasks.Listeners;

import info.beastsoftware.beastcore.BeastCore;
import info.beastsoftware.beastcore.listener.MobMergerListener;
import info.beastsoftware.beastcore.mobstacker.IStackedMob;
import info.beastsoftware.beastcore.mobstacker.StackedMob;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.util.Methods;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeastCoreListener implements Listener {
    private static BeastCoreListener instance;
    public Map<Entity, Integer> beastCoreStackedKill = new ConcurrentHashMap<>();

    public void Init() {
        instance = this;
    }

    public static BeastCoreListener getInstance() {
        return instance;
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onBeastCoreDeath(EntityDeathEvent event) {
        if (beastCoreStackedKill.containsKey(event.getEntity())) {
            final int finalStackKill = beastCoreStackedKill.get(event.getEntity());
            beastCoreStackedKill.remove(event.getEntity());

            IStackedMob stackedMob = MobMergerListener.getStackedMobsManager().getStack(event.getEntity());
            if(stackedMob == null){
                return;
            }

            List<ItemStack> drops = event.getDrops();
            {
                drops.stream().forEach(drop -> {
                    drop.setAmount(drop.getAmount() * finalStackKill);
                });
                event.setDroppedExp(event.getDroppedExp() * finalStackKill);

                if (stackedMob.getSize() > finalStackKill) {
                    IStackedMob newStack = new StackedMob(stackedMob.getSize() - finalStackKill, event.getEntity());
                    MobMergerListener.getStackedMobsManager().spawnStack(newStack, event.getEntity().getLocation(), event.getEntity().getType().name() + " " + newStack.getSize());
                }

                MobMergerListener.getStackedMobsManager().removeStack(stackedMob);
                stackedMob.getEntity().removeMetadata("MERGED", BeastCore.getInstance());
            }
        }
    }
}
