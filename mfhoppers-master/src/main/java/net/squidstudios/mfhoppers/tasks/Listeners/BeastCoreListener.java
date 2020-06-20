package net.squidstudios.mfhoppers.tasks.Listeners;

import info.beastsoftware.beastcore.BeastCore;
import info.beastsoftware.beastcore.entity.StackedMob;
import info.beastsoftware.beastcore.manager.MergedMobsManager;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeastCoreListener implements Listener {
    private static BeastCoreListener instance;
    public Map<Entity, Integer> beastCoreStackedKill = new ConcurrentHashMap<>();

    MergedMobsManager mobsManager;

    public void Init() {
        instance = this;
        final Class serviceClass = BeastCore.getInstance().getApi().getMobsService().getClass();
        Field managerField;
        try {
            managerField = serviceClass.getDeclaredField("mergedMobsManager");
            managerField.setAccessible(true);
            mobsManager = (MergedMobsManager) managerField.get(BeastCore.getInstance().getApi().getMobsService());
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static BeastCoreListener getInstance() {
        return instance;
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onBeastCoreDeath(final EntityDeathEvent event) {
        if (beastCoreStackedKill.containsKey(event.getEntity())) {
            final int finalStackKill = beastCoreStackedKill.get(event.getEntity());
            beastCoreStackedKill.remove(event.getEntity());

            final StackedMob stackedMob = mobsManager.fromEntity(event.getEntity());
            if(stackedMob == null){
                return;
            }

            final List<ItemStack> drops = event.getDrops();
            {
                drops.stream().forEach(drop -> {
                    drop.setAmount(drop.getAmount() * finalStackKill);
                });
                event.setDroppedExp(event.getDroppedExp() * finalStackKill);

                if (stackedMob.getSize() > finalStackKill) {
                    stackedMob.setSize(stackedMob.getSize() - finalStackKill);
                }
                //stackedMob.getEntity().removeMetadata(StackedMob.METADATA, BeastCore.getInstance());
            }
        }
    }
}
