//? if >= 26.3 {
/*package at.hannibal2.skyhanni.utils.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import java.util.function.Function;

// Preserve the26.2 translucent armor/glint setup using26.3 native pipeline/pass routing.
public final class NativeRenderTypes263 {
    private NativeRenderTypes263() {}
    private static RenderPipeline armorPipeline(boolean glint) {
        return RenderPipelines.register(RenderPipeline.builder(glint
            ? new RenderPipeline.Snippet[]{RenderPipelines.ENTITY_SNIPPET, RenderPipelines.GLINT_SNIPPET}
            : new RenderPipeline.Snippet[]{RenderPipelines.ENTITY_SNIPPET})
            .withLocation(Identifier.fromNamespaceAndPath("skyhanni", glint ? "armor_translucent_glint" : "armor_translucent"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withShaderDefine("NO_OVERLAY").withShaderDefine("PER_FACE_LIGHTING")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false).build());
    }
    private static final RenderPipeline ARMOR_PIPELINE = armorPipeline(false);
    private static final RenderPipeline ARMOR_GLINT_PIPELINE = armorPipeline(true);
    private static final java.util.function.BiFunction<Identifier, Boolean, RenderType> ARMOR = Util.memoize((texture, glint) -> {
        var setup = RenderSetup.builder(glint ? ARMOR_GLINT_PIPELINE : ARMOR_PIPELINE)
            .withTexture("Sampler0", texture).useLightmap().useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .affectsCrumbling().sortOnUpload().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        if (glint) setup.withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING);
        return RenderType.create("skyhanni_armor_translucent" + (glint ? "_glint" : ""), setup.createRenderSetup());
    });
    private static final RenderType GLINT = RenderType.create("skyhanni_glint_translucent",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING).createRenderSetup());

    public static RenderType armorTranslucent(Identifier texture, RenderType original) {
        return ARMOR.apply(texture, original.state.pipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT);
    }
    public static RenderType glintTranslucent() { return GLINT; }
}
*///?}
