package net.squidstudios.mfhoppers.hopper.types;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.util.Methods;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class GrindHopper extends IHopper {
    @Override
    public void save(PreparedStatement stat) {
        try {
            stat.setString(1, getData().get("name").toString());
            stat.setString(2, getData().get("loc").toString());
            stat.setInt(3, Integer.valueOf(getData().get("lvl").toString()));
            stat.setString(4, getData().get("ent").toString());
            stat.setBoolean(5, (boolean)getData().get("isAuto"));
            stat.setBoolean(6, (boolean)getData().get("isGlobal"));
            stat.setString(7, generateToSaveData());
            stat.addBatch();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    public GrindHopper(Location loc, String name, int lvl, EntityType type, boolean isAuto, boolean isGlobal){

        getData().put("ent", type);
        getData().put("type", HopperEnum.Grind.name());
        getData().put("name", name);
        getData().put("lvl", lvl);
        getData().put("loc", Methods.toString(loc));
        getData().put("isAuto", isAuto);
        getData().put("isGlobal", isGlobal);

    }
    public GrindHopper(Location loc, String name, int lvl, EntityType type, boolean isAuto, boolean isGlobal,Map<String, Object> data){

        getData().put("ent", type);
        getData().put("type", HopperEnum.Grind.name());
        getData().put("name", name);
        data.entrySet().forEach( entry -> getData().put(entry.getKey(), entry.getValue()));
        getData().put("lvl", lvl);
        getData().put("loc", Methods.toString(loc));
        getData().put("isAuto", isAuto);
        getData().put("isGlobal", isGlobal);

    }

    @Override
    public ItemStack getItem() {
        return null;
    }

    @Override
    public HopperEnum getType() {
       return HopperEnum.Grind;
    }

    public String generateToSaveData(){
        Map<String, Object> ret = new HashMap<>();

        if(getData().containsKey("linked")) ret.put("linked", getData().get("linked"));

        if(getData().containsKey("owner")) ret.put("owner", getData().get("owner"));

        return Methods.serialize(ret);
    }

    @Override
    public boolean isActive() {
        return super.isActive() && Methods.materialEqualsTo(this.getLocation().clone().add(0, 1, 0), Material.AIR, 2);
    }
}
