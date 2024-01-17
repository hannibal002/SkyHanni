package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(WorldClient.class)
public interface AccessorWorldClient {

    @Accessor("entityList")
    Set<Entity> getEntityList_skyhanni();
}
