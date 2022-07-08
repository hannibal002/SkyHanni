package com.thatgravyboat.skyblockhud_2.textures;

import com.google.gson.JsonObject;
import java.util.Arrays;
import net.minecraft.util.ResourceLocation;

public class TextureObject {

    public String displayName;
    public ResourceLocation bars = resource("bars.png");
    public ResourceLocation mines = resource("mines.png");
    public ResourceLocation playerStats = resource("playerstats.png");
    public ResourceLocation stats = resource("stats.png");
    public ResourceLocation dungeon = resource("dungeon.png");
    public ResourceLocation dialogue = resource("dialogue.png");

    public TextureObject(String displayName) {
        this.displayName = displayName;
    }

    public static TextureObject decode(JsonObject json) {
        TextureObject textureObject = new TextureObject(json.get("displayName").getAsString());
        Arrays
            .stream(textureObject.getClass().getDeclaredFields())
            .filter(field -> field.getType().equals(ResourceLocation.class))
            .forEach(field -> {
                try {
                    field.set(textureObject, new ResourceLocation(json.get(field.getName()).getAsString()));
                } catch (Exception ignored) {}
            });
        return textureObject;
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation("skyblockhud", path);
    }
}
