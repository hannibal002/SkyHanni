package at.hannibal2.skyhanni.data

enum class FameRank(
    val rank: String,
    val fameRequired: Long,
    val bitsMultiplier: Double,
    val electionVotes: Int,
) {
    NEW_PLAYER("New Player", 0, 1.0, 1),
    SETTLER("Settler", 20_000, 1.1, 1),
    CITIZEN("Citizen", 80_000, 1.2, 2),
    CONTRIBUTOR("Contributor", 200_000, 1.3, 3),
    PHILANTHROPIST("Philanthropist", 40_0000, 1.4, 5),
    PATRON("Patron", 800_000, 1.6, 10),
    FAMOUS_PLAYER("Famous Player", 1_500_000, 1.8, 20),
    ATTACHE("Attaché", 3_000_000, 1.9, 25),
    AMBASSADOR("Ambassador", 10_000_000, 2.0, 50),
    STATESPERSON("Statesperson", 20_000_000, 2.04, 75),
    SENATOR("Senator", 33_000_000, 2.08, 100),
    DIGNITARY("Dignitary", 50_000_000, 2.12, 100),
    COUNCILOR("Councilor", 72_000_000, 2.16, 100),
    MINISTER("Minister", 100_000_000, 2.2, 100),
    PREMIER("Premier", 135_000_000, 2.22, 100),
    CHANCELLOR("Chancellor", 178_000_000, 2.24, 100),
    SUPREME("Supreme", 230_000_000, 2.26, 100)
}
