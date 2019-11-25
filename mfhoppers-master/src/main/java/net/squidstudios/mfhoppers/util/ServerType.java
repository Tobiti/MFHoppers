package net.squidstudios.mfhoppers.util;

import java.util.Arrays;

public enum ServerType {

    PAPERSPIGOT("paperspigot"),
    SPIGOT("spigot"),
    TACOSPIGOT("tacospigot");

    private String version;
    ServerType(String version){
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static ServerType of(String v){
        return Arrays.stream(values()).filter(it -> it.version.equalsIgnoreCase(v)).findFirst().orElse(SPIGOT);
    }

}
