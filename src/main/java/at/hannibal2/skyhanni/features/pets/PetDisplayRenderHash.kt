package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.text.PetTextDisplaySettings
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.BorderRingConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig.IconRotationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.PetItemConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.RarityBackgroundConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.VisualPetDisplayConfig

private fun IconConfig.renderHash(): Int = listOf(
    enabled.get(),
    skinAnimation.get(),
    skinAnimationSpeed.get(),
    scale.get(),
    rotation.renderHash(),
).hashCode()

private fun IconRotationConfig.renderHash(): Int = listOf(
    staticRotation.xRotation.get(),
    staticRotation.yRotation.get(),
    staticRotation.zRotation.get(),
    spinRotation.speedX.get(),
    spinRotation.speedY.get(),
    spinRotation.speedZ.get(),
).hashCode()

private fun PetItemConfig.renderHash(): Int = listOf(
    enabled.get(),
    placement.get(),
    scale.get(),
).hashCode()

private fun VisualPetDisplayConfig.BackgroundColorConfig.renderHash(): Int = listOf(
    enabled.get(),
    customization.renderHash(),
    borderRing.renderHash(),
).hashCode()

private fun RarityBackgroundConfig.renderHash(): Int = listOf(
    padding.get(),
    commonColor.get(),
    uncommonColor.get(),
    rareColor.get(),
    epicColor.get(),
    legendaryColor.get(),
    mythicColor.get(),
).hashCode()

private fun BorderRingConfig.renderHash(): Int = listOf(
    enabled.get(),
    customization.padding.get(),
    customization.filledColor.get(),
    customization.unfilledColor.get(),
    separator.enabled.get(),
    separator.padding.get(),
    separator.color.get(),
).hashCode()

private fun ExpSharePetDisplayConfig.renderHash(): Int = listOf(
    enabled.get(),
    organization.placement.get(),
    organization.groupOrientation.get(),
    organization.subOrbit.orbitDistance.get(),
    organization.subOrbit.orbitDirection.get(),
    organization.subOrbit.orbitSpeed.get(),
    icon.renderHash(),
    icon.iconSpacing.get(),
    petItem.renderHash(),
    rarityBackground.renderHash(),
    activeSlotsOnly.get(),
    disabledOpacity.get(),
).hashCode()

private fun PetTextDisplaySettings.settingsRenderHash(): Int = listOf(
    enabledTextsProperty().get().toList(),
    textLabelsProperty().get(),
    nameLevelProperty().get(),
    nameSkinSymbolProperty().get(),
    nextLevelPercentProperty().get(),
    xpFormatProperty().get(),
    textScaleProperty().get(),
    textLocationProperty().get(),
    verticalAlignProperty().get(),
    horizontalAlignProperty().get(),
).hashCode()

private fun TextPetDisplayConfig.ExpSharePetTextConfig.renderHash(): Int = listOf(
    enabled.get(),
    textMode.get(),
    bundledLocation.get(),
    bundledSpacing.get(),
    settingsRenderHash(),
).hashCode()

private fun TextPetDisplayConfig.renderHash(): Int = listOf(
    equippedPet.settingsRenderHash(),
    equippedPet.centerTarget.get(),
    expSharePets.renderHash(),
).hashCode()

private fun VisualPetDisplayConfig.renderHash(): Int = listOf(
    icon.renderHash(),
    rarityBackground.renderHash(),
    petItem.renderHash(),
).hashCode()

internal fun PetDisplayConfig.renderHash(): Int = listOf(
    general.enabled.get(),
    visual.equippedPet.renderHash(),
    visual.expSharePets.renderHash(),
    text.renderHash(),
).hashCode()
