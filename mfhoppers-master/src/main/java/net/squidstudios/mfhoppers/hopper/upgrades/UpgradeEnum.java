package net.squidstudios.mfhoppers.hopper.upgrades;

public enum UpgradeEnum {
    XP("Experience"),
    COMMAND("Command"),
    ECO("Money");
    private String understandable;
    UpgradeEnum(String understandable){
        this.understandable = understandable;
    }

    public String getUnderstandable() {
        return understandable;
    }
}
