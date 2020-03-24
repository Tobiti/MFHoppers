package net.squidstudios.mfhoppers.util;

import java.util.Comparator;

import org.bukkit.entity.EntityType;

public class EntityTypeComparator implements Comparator<EntityType> {

    @Override
    public int compare(EntityType first, EntityType second) {
        return first.name().compareTo(second.name());
    }
}