package net.squidstudios.mfhoppers.hopper.convert;

import net.squidstudios.mfhoppers.hopper.upgrades.UpgradeEnum;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityPrice {

    private int price;
    private EntityType entityType;
    private UpgradeEnum priceType;

    EntityPrice(int price, EntityType entityType, UpgradeEnum priceType){
        this.price = price;
        this.entityType = entityType;
        this.priceType = priceType;
    }

    public static EntityPrice decode(String split[]){
        if(split.length < 3) {
            int price = Integer.valueOf(split[1]);
            EntityType entityType = EntityType.valueOf(split[0]);
            return new EntityPrice(price, entityType, UpgradeEnum.ECO);
        }
        else {
            int price = Integer.valueOf(split[2]);
            EntityType entityType = EntityType.valueOf(split[0]);
            return new EntityPrice(price, entityType, UpgradeEnum.valueOf(split[1].toUpperCase()));
        }

    }

    public UpgradeEnum getPriceType(){
        return  priceType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public int getPrice() {
        return price;
    }
    public static Map<EntityType, EntityPrice> decode(List<String> decode){
        Map<EntityType, EntityPrice> ret = new HashMap<>();
        for(String de : decode){
            String split[] = de.split(":");
            EntityType entityType = EntityType.valueOf(split[0]);
            ret.put(entityType, decode(split));
        }
        return ret;
    }
}
