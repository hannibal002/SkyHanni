package at.hannibal2.skyhanni.utils.repopatterns

class RepoInteger private constructor(key: String, fallback: String, parent: RepoPatternKeyOwner? = null) :
    BaseSingleRepoValue<Int>(key, fallback, parent) {

    override fun parse(raw: String): Int = raw.toInt()

    companion object {
        /** Factory method for a single String */
        fun integer(key: String, fallback: Int, parent: RepoPatternKeyOwner? = null): RepoInteger {
            return RepoInteger(key, fallback.toString(), parent)
        }

        /** Factory method for a List of Strings */
        fun list(key: String, vararg fallbacks: Int, parent: RepoPatternKeyOwner? = null): RepoIntegerList {
            return RepoIntegerList(key, fallbacks.map { it.toString() }, parent)
        }
    }
}


class RepoIntegerList internal constructor(key: String, fallbacks: List<String>, parent: RepoPatternKeyOwner? = null) :
    BaseListRepoValue<Int>(key, fallbacks, parent) {
    override fun parse(raw: String): Int = raw.toInt()
}

class RepoDouble private constructor(key: String, fallback: String, parent: RepoPatternKeyOwner? = null) :
    BaseSingleRepoValue<Double>(key, fallback, parent) {

    override fun parse(raw: String): Double = raw.toDouble()

    companion object {
        /** Factory method for a single String */
        fun double(key: String, fallback: Double, parent: RepoPatternKeyOwner? = null): RepoDouble {
            return RepoDouble(key, fallback.toString(), parent)
        }

        /** Factory method for a List of Strings */
        fun list(key: String, vararg fallbacks: Double, parent: RepoPatternKeyOwner? = null): RepoDoubleList {
            return RepoDoubleList(key, fallbacks.map { it.toString() }, parent)
        }
    }
}

class RepoDoubleList internal constructor(key: String, fallbacks: List<String>, parent: RepoPatternKeyOwner? = null) :
    BaseListRepoValue<Double>(key, fallbacks, parent) {
    override fun parse(raw: String): Double = raw.toDouble()
}
