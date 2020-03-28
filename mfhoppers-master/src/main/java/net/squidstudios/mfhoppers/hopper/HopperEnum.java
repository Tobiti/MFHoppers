package net.squidstudios.mfhoppers.hopper;

public enum HopperEnum {
    Break(new String[]{"type", "limitPerChunk", "name", "lore", "should_drop_from_explosions", "breakEvery", "drops"}, new String[]{"limitPerChunk", "lore", "name", "breakEvery","drops"}),
    Grind(new String[]{"type", "limitPerChunk", "name", "lore", "should_drop_from_explosions", "mob", "damage", "time"},new String[]{"limitPerChunk", "lore", "name", "defaultMob", "damage", "time"}),
    Crop(new String[]{"type", "limitPerChunk", "name", "lore", "should_drop_from_explosions", "crops"}, new String[]{"limitPerChunk", "lore", "name", "defaultMob", "crops"}),
    Mob(new String[]{"type", "limitPerChunk", "name", "lore", "should_drop_from_explosions", "drops"}, new String[]{"limitPerChunk", "lore", "name", "defaultMob", "drops"}),
    Normal(new String[]{"type", "limitPerChunk", "name", "lore", "should_drop_from_explosions"}, new String[]{"limitPerChunk", "lore", "name"});;
    private String[] req = {""};
    private String[] upgrades = {""};
    HopperEnum(String[] req){

        this.req = req;

    }
    HopperEnum(String[] req, String[] keys_that_can_be_upgrades){

        this.req = req;
        this.upgrades = keys_that_can_be_upgrades;

    }
    public String[] getReq() {
        return req;
    }


    public static HopperEnum match(String s){

        for(HopperEnum e : values()){

            if(e.name().equalsIgnoreCase(s)){
                return e;
            }

        }
        return null;

    }

    public String[] getUpgrades() {
        return upgrades;
    }
}
