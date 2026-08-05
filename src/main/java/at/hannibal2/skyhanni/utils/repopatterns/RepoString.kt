package at.hannibal2.skyhanni.utils.repopatterns


class RepoString private constructor(key: String, fallback: String, parent: RepoPatternKeyOwner? = null) :
    BaseSingleRepoValue<String>(key, fallback, parent) {

    override fun parse(raw: String): String = raw

    companion object {
        /** Factory method for a single String */
        fun string(key: String, fallback: String, parent: RepoPatternKeyOwner? = null): RepoString {
            return RepoString(key, fallback, parent)
        }

        /** Factory method for a List of Strings */
        fun list(key: String, vararg fallbacks: String, parent: RepoPatternKeyOwner? = null): RepoStringList {
            return RepoStringList(key, fallbacks.toList(), parent)
        }
    }
}

class RepoStringList internal constructor(key: String, fallbacks: List<String>, parent: RepoPatternKeyOwner? = null) :
    BaseListRepoValue<String>(key, fallbacks, parent) {
    override fun parse(raw: String): String = raw
}
