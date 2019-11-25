package net.squidstudios.mfhoppers.util;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;

public class MMaterial {
    public static Material matchMaterial(String mat){

        //first check
        try{
            return Material.getMaterial(mat);
        } catch (Exception ex){

        }
        //Second check
        mat = "LEGACY_" + StringUtils.upperCase(mat);
        try{
            return Material.valueOf(mat);
        } catch (Exception ex){

            ex.printStackTrace();

        }
        return null;
    }
}
