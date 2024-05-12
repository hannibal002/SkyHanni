package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.annotations.Expose

data class MayorJson(
    @Expose val mayor: MayorInfo,
    @Expose val current: MayorElection?,
)

data class MayorInfo(
    @Expose val key: String,
    @Expose val name: String,
    @Expose val perks: List<MayorPerk>,
    @Expose val election: MayorElection,
)

data class MayorElection(
    @Expose val year: Int,
    @Expose val candidates: List<MayorCandidate>,
)

data class MayorCandidate(
    @Expose val key: String,
    @Expose val name: String,
    @Expose val perks: List<MayorPerk>,
    @Expose val votes: Int,
)

data class MayorPerk(
    @Expose val name: String,
    @Expose val description: String,
)
