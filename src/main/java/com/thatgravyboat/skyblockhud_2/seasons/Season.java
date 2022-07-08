package com.thatgravyboat.skyblockhud_2.seasons;

public enum Season {
    EARLYSPRING("earlyspring", "Early Spring", 34, 0),
    SPRING("spring", "Spring", 34, 31),
    LATESPRING("latespring", "Late Spring", 34, 62),
    EARLYSUMMER("earlysummer", "Early Summer", 42, 93),
    SUMMER("summer", "Summer", 42, 124),
    LATESUMMER("latesummer", "Late Summer", 42, 155),
    EARLYAUTUMN("earlyautumn", "Early Autumn", 50, 186),
    AUTUMN("autumn", "Autumn", 50, 217),
    LATEAUTUMN("lateautumn", "Late Autumn", 50, 248),
    EARLYWINTER("earlywinter", "Early Winter", 58, 279),
    WINTER("winter", "Winter", 58, 310),
    LATEWINTER("latewinter", "Late Winter", 58, 341),
    ERROR("error", "Error", 0, -1);

    private final String name;
    private final String displayName;
    private final int textureX;
    private final int yearStartDay;

    Season(String name, String displayName, int textureX, int yearStartDay) {
        this.name = name;
        this.displayName = displayName;
        this.textureX = textureX;
        this.yearStartDay = yearStartDay;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getTextureX() {
        return this.textureX;
    }

    public int getYearStartDay() {
        return yearStartDay;
    }

    public static Season get(String id) {
        try {
            return Season.valueOf(id);
        } catch (IllegalArgumentException ex) {
            return ERROR;
        }
    }
}
