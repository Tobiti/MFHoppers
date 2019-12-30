package net.squidstudios.mfhoppers.util;

import net.squidstudios.mfhoppers.MFHoppers;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;

public enum GlassColor {

    BLACK(15),
    BLUE(11),
    BROWN(12),
    CYAN(9),
    GRAY(7),
    GREEN(13),
    LIGHT_BLUE(3),
    LIME(5),
    MAGENTA(2),
    ORANGE(1),
    PINK(6),
    PURPLE(10),
    RED(14),
    WHITE(0),
    YELLOW(4);

    private int glass_id = 0;

    GlassColor(int id){
        this.glass_id = id;
    }

    public ItemStack getItem() {
        if (OVersion.isOrAfter(13)) {

            String name = name() + "_STAINED_GLASS_PANE";
            return new ItemBuilder(XMaterial.fromString(name).parseMaterial()).
                    addItemFlag(ItemFlag.HIDE_ATTRIBUTES).
                    addItemFlag(ItemFlag.HIDE_ENCHANTS).
                    addNbt("filler", "filler").
                    setName(" ").buildItem();

        } else{

            Material mat = MMaterial.matchMaterial("STAINED_GLASS_PANE");
            return new ItemBuilder(mat).
                    addNbt("filler", "filler").
                    setName(" ").
                    setDurability((byte)this.glass_id).buildItem();

        }

    }
}
