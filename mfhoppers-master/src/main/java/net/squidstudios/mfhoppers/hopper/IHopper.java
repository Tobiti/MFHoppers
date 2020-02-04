package net.squidstudios.mfhoppers.hopper;

import com.google.common.collect.Sets;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.util.MContainer;
import net.squidstudios.mfhoppers.util.Methods;
import net.squidstudios.mfhoppers.util.OFuture;
import net.squidstudios.mfhoppers.util.XMaterial;
import net.squidstudios.mfhoppers.util.plugin.Tasks;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

public abstract class IHopper {

    public static class FilterElement {
        public Material Material;
        public boolean HasDamageValue = false;
        public short DamageValue = -1;

        public FilterElement(Material material, boolean hasDamageValue, short damageValue) {
            this.DamageValue = damageValue;
            this.HasDamageValue = hasDamageValue;
            this.Material = material;
        }
    }

    private HashMap<String, Object> data = new HashMap<>();
    private Set<FilterElement> filterList = Sets.newConcurrentHashSet();

    private InventoryHolder cached_inventory;
    private Chunk cached_chunk;

    public abstract void save(PreparedStatement stat);

    public HashMap<String, Object> getData() {
        return data;
    }

    public Location getLocation() {
        if (getData().containsKey("cachedLocation")) {
            return ((Location) data.get("cachedLocation"));

        } else {
            Location location = Methods.toLocation(data.get("loc").toString());
            data.put("cachedLocation", location);
            return location;
        }
    }

    public Chunk getChunk() {
        if (cached_chunk != null)
            return cached_chunk;

        if (Thread.currentThread().getName().equalsIgnoreCase("Server thread")) {
            cached_chunk = getLocation().getChunk();
            return cached_chunk;

        } else {
            CompletableFuture<Chunk> callback = new CompletableFuture<>();
            Tasks.getInstance().runTask(() -> {
                cached_chunk = getLocation().getChunk();
                callback.complete(cached_chunk);
            });

            try {
                return callback.get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;

        }
    }

    public abstract ItemStack getItem();

    public String getName() {
        return data.get("name").toString();
    }

    public abstract HopperEnum getType();

    public int getLevel() {
        return (int) data.get("lvl");
    }

    public CompletableFuture<Inventory> getInventory() {
        if (cached_inventory != null) {
            OFuture<Inventory> future = new OFuture<>();
            future.complete(isChunkLoaded() ? cached_inventory.getInventory() : null);
            return future;

        } else if (Thread.currentThread().getName().equalsIgnoreCase("Server Thread")) {
            OFuture<Inventory> future = new OFuture<>();
            if (getLocation().getBlock().getState() instanceof InventoryHolder) {
                cached_inventory = ((InventoryHolder) getLocation().getBlock().getState());
                future.complete(cached_inventory.getInventory());
            }
            future.complete(null);
            return future;

        } else {
            CompletableFuture<Inventory> future = new CompletableFuture<>();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (getLocation().getBlock().getState() instanceof InventoryHolder) {
                        cached_inventory = ((InventoryHolder) getLocation().getBlock().getState());
                        future.complete(cached_inventory.getInventory());
                    }
                    future.complete(null);
                }
            }.runTask(MFHoppers.getInstance());
            return future;
        }
    }

    public Boolean isLinked() {
        return data.containsKey("linked");
    }

    public List<Location> getLinked() {
        if (!isLinked()) return new ArrayList<>();

        return ((List<String>) getData().get("linked")).stream().map(location -> Methods.toLocation(location)).filter(it -> it != null).collect(toList());

    }

    public List<String> getLinkedAsStrings() {
        return ((List<String>) getData().get("linked"));

    }

    public ConfigHopper getConfigHopper() {
        return MFHoppers.getInstance().getConfigHoppers().get(getName());
    }

    public boolean isLinkedTo(Location location) {
        if (MContainer.isDoubleChest(location)) {

            Chest chest = ((Chest) location.getBlock().getState());
            DoubleChestInventory doubleChest = ((DoubleChestInventory) chest.getInventory());

            Location loc1 = MContainer.getFromHolder(doubleChest.getLeftSide().getHolder()).getLocation(doubleChest.getLeftSide().getHolder());
            Location loc2 = MContainer.getFromHolder(doubleChest.getLeftSide().getHolder()).getLocation(doubleChest.getRightSide().getHolder());

            return getLinked().stream().anyMatch(loc -> loc != null && loc.getWorld() == loc1.getWorld() && (loc.distance(loc1) <= 0.5 || loc.distance(loc2) <= 0.5));

        } else return getLinked().contains(location);
    }

    public boolean isChunkLoaded() {
        int chunkX = getLocation().getBlockX() >> 4;
        int chunkZ = getLocation().getBlockZ() >> 4;

        Location location = getLocation();
        if (location.getWorld() == null)
            return false;

        return location.getWorld().isChunkLoaded(chunkX, chunkZ) || Methods.containsPlayersAroundHopper(location);
    }

    public void unlink(Location location) {
        List<String> locations = getLinkedAsStrings();

        locations.remove(Methods.toString(location));
        getData().remove("linked");
        getData().put("linked", locations);
        
        DataManager.getInstance().update(this);
    }

    public boolean ContainsInFilterMaterialList(Material mat, short damage) {
        Set<FilterElement> filter = this.getFilterMaterialList();

        return filter.stream().anyMatch(filterElement -> {
            return (!filterElement.HasDamageValue && filterElement.Material == mat) || (filterElement.Material == mat && filterElement.DamageValue == damage);
        });
    }

    public void ResetFilterList() {
        if (getType() == HopperEnum.Crop || getType() == HopperEnum.Mob) {
            filterList.clear();
            getFilterMaterialList(true);
            DataManager.getInstance().update(this);
        }
    }

    public Set<FilterElement> getFilterMaterialList() {
        return getFilterMaterialList(false);
    }

    public boolean hasEditableFilter(){
        return MFHoppers.getInstance().getConfigHoppers().get(getData().get("name").toString()).isEditableFilter();
    }

    public Set<FilterElement> getFilterMaterialList(boolean forceConfig) {
        if (filterList.size() == 0) {
            List<String> _stringMats = new ArrayList<>();
            if (!hasEditableFilter() || !getData().containsKey("filter") || forceConfig) {
                if (getType() == HopperEnum.Crop) {
                    _stringMats = (List<String>) MFHoppers.getInstance().getConfigHoppers().get(getData().get("name").toString()).getDataOfHopper(this).get("crops");
                } else {
                    _stringMats = (List<String>) MFHoppers.getInstance().getConfigHoppers().get(getData().get("name").toString()).getDataOfHopper(this).get("drops");
                }
                getData().put("filter", _stringMats);
            } else {
                if (getData().containsKey("filter")) {
                    _stringMats = (List<String>) getData().get("filter");
                }
            }
            for (String s : _stringMats) {
                String[] parts = s.split(":");
                Material mat = Material.getMaterial(parts[0]);
                if (mat == null) {
                    Optional<XMaterial> optMat = XMaterial.matchXMaterial(s);
                    if (optMat.isPresent()) {
                        mat = optMat.get().parseMaterial();
                    } else {
                        MFHoppers.getInstance().getLogger().warning("Could not find Material to " + s);
                    }
                }
                if (mat != null) {
                    if (parts.length > 1) {
                        filterList.add(new FilterElement(mat, true, Short.valueOf(parts[1])));
                    } else {
                        filterList.add(new FilterElement(mat, false, (short) -1));
                    }
                } else {
                    MFHoppers.getInstance().getLogger().warning("Could not find Material to " + s);
                }
            }
        }

        return filterList;
    }

    public void SetFilterMaterialList(Set<FilterElement> mats) {
        filterList = mats;
        List<String> _stringMats = new ArrayList<>();
        for (FilterElement element : mats) {
            if (!element.HasDamageValue) {
                _stringMats.add(element.Material.toString());
            } else {
                _stringMats.add(element.Material.toString() + ":" + element.DamageValue);
            }
        }
        getData().put("filter", _stringMats);

        // Update Hopper
        DataManager.getInstance().update(this);
    }

    public void link(Location loc) {
        if (getData().containsKey("linked")) {

            List<String> locations = ((List<String>) getData().get("linked"));
            locations.add(Methods.toString(loc));
            getData().remove("linked");
            getData().put("linked", locations);

        } else {

            List<String> locations = new ArrayList<>();
            locations.add(Methods.toString(loc));
            getData().remove("linked");
            getData().put("linked", locations);

        }
        DataManager.getInstance().update(this);
    }

    public String getOwner() {

        if (getData().containsKey("owner")) return getData().get("owner").toString();
        return null;

    }

    public boolean isActive() {
        return true;
    }

    public void recacheInventory() {
        this.cached_inventory = null;
        getInventory();
    }

}
