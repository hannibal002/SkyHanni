package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.hasNameTagWith
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntitySkeleton
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class SeaCreatureFeatures {
    private val config get() = SkyHanniMod.feature.fishing.rareCatches
    private var rareSeaCreatures = listOf<EntityLivingBase>()
    private var lastRareCatch = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return
        val entity = event.entity as? EntityLivingBase ?: return
        if (DamageIndicatorManager.isBoss(entity)) return

        val maxHealth = event.maxHealth
        for (creatureType in RareSeaCreatureType.entries) {
            if (!creatureType.health.any { entity.hasMaxHealth(it, false, maxHealth) }) continue
            if (!creatureType.clazz.isInstance(entity)) continue
            if (!entity.hasNameTagWith(3, creatureType.nametag)) continue

            rareSeaCreatures = rareSeaCreatures.editCopy { add(entity) }
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.RED.toColor().withAlpha(50))
            { config.highlight }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.highlight }

            if (creatureType == RareSeaCreatureType.WATER_HYDRA && entity.health == (entity.baseMaxHealth.toFloat() / 2)) continue; // Water hydra splitting in two
            if (config.alertOtherCatches && lastRareCatch.passedSince() > 1.seconds) {
                val creature = SeaCreatureManager.allFishingMobs[creatureType.nametag]
                TitleManager.sendTitle("${creature?.rarity?.chatColorCode ?: "§6"}RARE SEA CREATURE!", 1.5.seconds, 3.6, 7.0)
                if (config.playSound) SoundUtils.playBeepSound()
            }
        }
    }

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.alertOwnCatches) return

        if (event.seaCreature.rare) {
            TitleManager.sendTitle("${event.seaCreature.rarity.chatColorCode}RARE CATCH!", 3.seconds, 2.8, 7.0)
            if (config.playSound) SoundUtils.playBeepSound()
            lastRareCatch = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        rareSeaCreatures = emptyList()
    }

    @SubscribeEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && config.highlight && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline(getEntityOutlineColor)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.rareSeaCreatureHighlight", "fishing.rareCatches.highlight")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight

    private val getEntityOutlineColor: (entity: Entity) -> Int? = { entity ->
        if (entity is EntityLivingBase && entity in rareSeaCreatures && entity.distanceToPlayer() < 30) {
            LorenzColor.GREEN.toColor().rgb
        } else null
    }

    enum class RareSeaCreatureType(
        val clazz: Class<out EntityLivingBase>,
        val nametag: String,
        vararg val health: Int
    ) {
        WATER_HYDRA(EntityZombie::class.java, "Water Hydra", 500_000),
        SEA_EMPEROR(EntityGuardian::class.java, "Sea Emperor", 750_000, 800_000),
        SEA_EMPEROR_RIDER(EntitySkeleton::class.java, "Sea Emperor", 750_000, 800_000),
        ZOMBIE_MINER(EntityPlayer::class.java, "Zombie Miner", 2_000_000),
        PHANTOM_FISHERMAN(EntityPlayer::class.java, "Phantom Fisher", 1_000_000),
        GRIM_REAPER(EntityPlayer::class.java, "Grim Reaper", 3_000_000),
        YETI(EntityPlayer::class.java, "Yeti", 2_000_000),
        NUTCRACKER(EntityZombie::class.java, "Nutcracker", 4_000_000),
        GREAT_WHITE_SHARK(EntityPlayer::class.java, "Great White Shark", 1_500_000),
        THUNDER(EntityGuardian::class.java, "Thunder", 35_000_000),
        LORD_JAWBUS(EntityIronGolem::class.java, "Lord Jawbus", 100_000_000),
        PLHLEGBLAST(EntitySquid::class.java, "Plhlegblast", 500_000_000),
        ;
    }
}
