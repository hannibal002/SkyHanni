package at.lorenz.mod.config.textures;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.client.resources.IResource;
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
            //            LorenzMod.config.misc.style = 0;
        } else {
            texture = styles.get(selected);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        styles.clear();
        styles.add(DEFAULT_TEXTURE);
        DEFAULT_TEXTURE.displayName = "Default";
        try {
            ResourceLocation stylesData = new ResourceLocation("lorenzmod:data/styles.json");

            for (IResource resource : resourceManager.getAllResources(stylesData)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                    for (JsonElement json : jsonObject.getAsJsonArray("styles")) {
                        styles.add(TextureObject.decode((JsonObject) json));
                    }
                    if (DEFAULT_TEXTURE.displayName.equals("Default") && jsonObject.has("defaultDisplayName") && jsonObject.get("defaultDisplayName").isJsonPrimitive()) {
                        DEFAULT_TEXTURE.displayName = jsonObject.get("defaultDisplayName").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}
        //        if (LorenzMod.config != null) setTexture(LorenzMod.config.misc.style);
    }
}
