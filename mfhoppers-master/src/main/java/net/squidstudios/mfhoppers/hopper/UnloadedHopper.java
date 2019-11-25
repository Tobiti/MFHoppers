package net.squidstudios.mfhoppers.hopper;

import net.squidstudios.mfhoppers.hopper.types.BreakHopper;
import net.squidstudios.mfhoppers.hopper.types.CropHopper;
import net.squidstudios.mfhoppers.hopper.types.GrindHopper;
import net.squidstudios.mfhoppers.hopper.types.MobHopper;
import net.squidstudios.mfhoppers.util.Methods;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnloadedHopper{

    private Map<String, Object> data;
    private String worldName;
    private HopperEnum type;

    public UnloadedHopper(Map<String, Object> data, String worldName, HopperEnum type){
        this.data = data;
        this.worldName = worldName;
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getWorldName(){
        return worldName;
    }

    public IHopper build() {
        Location loc = Methods.toLocation(data.get("loc").toString());

        String name = data.get("name").toString();
        Map<String, Object> data2 = Methods.deserialize(data.get("data").toString());

        if(data2.containsKey("linked")){

            Object linked = data2.get("linked");

            if(Methods.toLocation(linked.toString()) == null){

                List<String> locations = (List<String>)linked;
                data2.remove("linked");
                data2.put("linked", locations);

            } else{

                List<String> locations = new ArrayList<>();
                locations.add(linked.toString());
                data2.remove("linked");
                data2.put("linked", locations);

            }

        }

        int level = (int)data.get("lvl");

        if(getType() == HopperEnum.Grind){

            boolean isAuto = (int)data.get("isAuto") == 1;
            boolean isGlobal = (int)data.get("isGlobal") == 1;
            EntityType type = EntityType.valueOf(data.get("ent").toString());

            return new GrindHopper(loc, name, level, type,isAuto,isGlobal,data2);

        } else if(getType() == HopperEnum.Break){
            return new BreakHopper(loc,name,level,data2);
        } else if(getType() == HopperEnum.Crop){
            return new CropHopper(loc,name,level, data2);
        } else if(getType() == HopperEnum.Mob){
            return new MobHopper(loc,name,level,data2);
        }
        return null;
    }

    public HopperEnum getType() {
        return type;
    }
}
