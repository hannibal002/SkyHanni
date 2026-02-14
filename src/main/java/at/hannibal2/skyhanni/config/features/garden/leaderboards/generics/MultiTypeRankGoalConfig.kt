package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

abstract class MultiTypeRankGoalConfig<E : Enum<E>, Config : TypeRankGoalGenericConfig<E>>(
    createConfig: () -> Config,
) : MultiModeTypeRankGoalConfig<E, Config, Config>(createConfig, createConfig)
