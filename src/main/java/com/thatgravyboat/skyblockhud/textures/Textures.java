package com.thatgravyboat.skyblockhud.textures;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thatgravyboat.skyblockhud.SkyblockHud;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

public class Textures implements IResourceManagerReloadListener {

    private static final TextureObject DEFAULT_TEXTURE = new TextureObject("Default");

    private static final Gson gson = new GsonBuilder().create();
    public static final List<TextureObject> styles = Lists.newArrayList(DEFAULT_TEXTURE);
    public static TextureObject texture = DEFAULT_TEXTURE;

    public static void setTexture(int selected) {
        if (selected >= styles.size() || selected < 0) {
            texture = DEFAULT_TEXTURE;
            SkyblockHud.config.misc.style = 0;
        } else {
            texture = styles.get(selected);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        styles.clear();
        styles.add(DEFAULT_TEXTURE);
        try {
            ResourceLocation stylesData = new ResourceLocation("skyblockhud:data/styles.json");
            InputStream is = resourceManager.getResource(stylesData).getInputStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                for (JsonElement json : gson.fromJson(reader, JsonObject.class).getAsJsonArray("styles")) {
                    styles.add(TextureObject.decode((JsonObject) json));
                }
            }
        } catch (Exception ignored) {}
    }
}
