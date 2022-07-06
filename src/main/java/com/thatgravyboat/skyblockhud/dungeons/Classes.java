package com.thatgravyboat.skyblockhud.dungeons;

public enum Classes {
    H("Healer", "H", 154),
    M("Mage", "M", 90),
    B("Berserk", "B", 122),
    A("Archer", "A", 58),
    T("Tank", "T", 186);

    private final String displayName;
    private final String classId;
    private final int textureY;

    Classes(String name, String id, int textureY) {
        this.displayName = name;
        this.classId = id;
        this.textureY = textureY;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getClassId() {
        return this.classId;
    }

    public int getTextureY() {
        return this.textureY;
    }

    public static Classes findClass(String input) {
        if (input.length() == 1) {
            try {
                return Classes.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        } else if (input.length() == 3) {
            try {
                return Classes.valueOf(input.replace("[", "").replace("]", "").toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        } else {
            for (Classes clazz : Classes.values()) {
                if (clazz.displayName.equalsIgnoreCase(input)) return clazz;
            }
        }
        return B;
    }
}
