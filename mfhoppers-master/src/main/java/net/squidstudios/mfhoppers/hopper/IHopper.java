package net.squidstudios.mfhoppers.hopper;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.util.Methods;
import net.squidstudios.mfhoppers.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import net.squidstudios.mfhoppers.util.MContainer;
import net.squidstudios.mfhoppers.util.plugin.Tasks;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Filter;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public abstract class IHopper {

    public static class FilterElement
    {
        public Material Material;
        public boolean HasDamageValue = false;
        public short DamageValue = -1;

        public FilterElement(Material material, boolean hasDamageValue, short damageValue)
        {
            this.DamageValue = damageValue;
            this.HasDamageValue = hasDamageValue;
            this.Material = material;
        }
    }

    private HashMap<String, Object> data = new HashMap<>();
    private List<FilterElement> filterList = new ArrayList<>();

    public abstract void save(PreparedStatement stat);

    public HashMap<String, Object> getData() {
        return data;
    }
    public Location getLocation(){

        if(getData().containsKey("cachedLocation")){

            return ((Location) data.get("cachedLocation"));

        } else{
            Location location = Methods.toLocation(data.get("loc").toString());
            data.put("cachedLocation", location);
            return location;
        }

    }

    public Chunk getChunk(){


        CompletableFuture<Chunk> callback = new CompletableFuture<>();

        Tasks.getInstance().runTask(() -> {
            callback.complete(getLocation().getChunk());
        });

        try {
            return callback.get();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return null;

    }
    public abstract ItemStack getItem();
    public String getName(){

        return data.get("name").toString();

    }
    public abstract HopperEnum getType();
    public int getLevel(){
        return (int)data.get("lvl");
    }
    public Inventory getInventory(){

        if(MContainer.getOfLocation(getLocation()) != null) return MContainer.getOfLocation(getLocation()).getInventory(getLocation());
        return null;

    }
    public Boolean isLinked(){
        return data.containsKey("linked");
    }
    public List<Location> getLinked(){

        if(!isLinked()) return new ArrayList<>();

        return ((List<String>) getData().get("linked")).stream().map(location -> Methods.toLocation(location)).filter(it -> it != null).collect(toList());

    }

    public List<String> getLinkedAsStrings(){

        return ((List<String>) getData().get("linked"));

    }

    public ConfigHopper getConfigHopper(){
        return MFHoppers.getInstance().getConfigHoppers().get(getName());
    }

    public boolean isLinkedTo(Location location) {

        if(MContainer.isDoubleChest(location)){

            Chest chest = ((Chest) location.getBlock().getState());

            DoubleChestInventory doubleChest = ((DoubleChestInventory) chest.getInventory());

            Location loc1 = MContainer.getFromHolder(doubleChest.getLeftSide().getHolder()).getLocation(doubleChest.getLeftSide().getHolder());
            Location loc2 = MContainer.getFromHolder(doubleChest.getLeftSide().getHolder()).getLocation(doubleChest.getRightSide().getHolder());

            return getLinked().stream().anyMatch(loc -> loc != null && loc.getWorld() == loc1.getWorld() && (loc.distance(loc1) <= 0.5 || loc.distance(loc2) <= 0.5));

        } else return getLinked().contains(location);

    }

    public boolean isChunkLoaded(){

        int chunkX = getLocation().getBlockX() >> 4;
        int chunkZ = getLocation().getBlockZ() >> 4;

        boolean toReturn = getLocation().getWorld().isChunkLoaded(chunkX, chunkZ);

        if(toReturn) {
            if (Methods.containsPlayersAroundHopper(getLocation())) {
                return true;
            }
        }

        return false;

    }

    public void unlink(Location location) {

        List<String> locations = getLinkedAsStrings();

        if(locations.contains(Methods.toString(location))){

            locations.remove(Methods.toString(location));

        }
        getData().remove("linked");
        getData().put("linked", locations);

    }

    public boolean ContainsInFilterMaterialList(Material mat, short damage)
    {
        List<FilterElement> filter = this.getFilterMaterialList();

        return filter.stream().anyMatch(filterElement -> {
            return (!filterElement.HasDamageValue && filterElement.Material == mat) || (filterElement.Material == mat && filterElement.DamageValue == damage);
        });
    }

    public void ResetFilterList(){
        if(getType() == HopperEnum.Crop || getType() == HopperEnum.Mob) {
            filterList.clear();
            getFilterMaterialList(true);
        }
    }

    public List<FilterElement> getFilterMaterialList()
    {
        return getFilterMaterialList(false);
    }

    public List<FilterElement> getFilterMaterialList(boolean forceConfig)
    {
        if(filterList.size() == 0) {
            List<String> _stringMats = new ArrayList<>();
            if (!getData().containsKey("filter") || forceConfig) {
                if (getType() == HopperEnum.Crop) {
                    _stringMats = (List<String>) MFHoppers.getInstance().getConfigHoppers().get(getData().get("name").toString()).getDataOfHopper(this).get("crops");
                } else {
                    _stringMats = (List<String>) MFHoppers.getInstance().getConfigHoppers().get(getData().get("name").toString()).getDataOfHopper(this).get("drops");
                }
                getData().put("filter", _stringMats);
            } else {
                if(getData().containsKey("filter")) {
                    _stringMats = (List<String>) getData().get("filter");
                }
            }
            for( String s : _stringMats){
                String[] parts = s.split(":");
                Material mat = Material.getMaterial(parts[0]);
                if(mat == null){
                    if(XMaterial.fromString(s) != null) {
                        mat = XMaterial.fromString(s).parseMaterial();
                    }
                    else {
                        MFHoppers.getInstance().getLogger().warning("Could not find Material to " + s);
                    }
                }
                if(mat != null) {
                    if(parts.length > 1) {
                        filterList.add(new FilterElement(mat, true, Short.valueOf(parts[1])));
                    }
                    else {
                        filterList.add(new FilterElement(mat, false, (short)-1));
                    }
                }
                else {
                    MFHoppers.getInstance().getLogger().warning("Could not find Material to " + s);
                }
            }
        }

        return filterList;
    }

    public void SetFilterMaterialList(List<FilterElement> mats)
    {
        filterList = mats;
        List<String> _stringMats = new ArrayList<>();
        for (FilterElement element : mats) {
            if(!element.HasDamageValue) {
                _stringMats.add(element.Material.toString());
            }
            else {
                _stringMats.add(element.Material.toString() + ":" + element.DamageValue);
            }
        }
        DataManager.getInstance().updateHopper(this);
        getData().put("filter", _stringMats);
    }

    public void link(Location loc) {

        if(getData().containsKey("linked")){

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

    }

    public String getOwner() {

        if(getData().containsKey("owner")) return getData().get("owner").toString();
        return null;

    }

}
