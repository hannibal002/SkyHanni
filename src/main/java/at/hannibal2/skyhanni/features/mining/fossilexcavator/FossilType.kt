package at.hannibal2.skyhanni.features.mining.fossilexcavator

enum class FossilType(
    val totalTiles: Int,
    val firstPercentage: String,
    val fossilShape: FossilShape,
    val possibleMutations: List<FossilMutation>
) {
    TUSK(
        8, "12.5%",
        FossilShape(
            listOf(
                FossilTile(0, 2),
                FossilTile(0, 3),
                FossilTile(0, 4),
                FossilTile(1, 1),
                FossilTile(2, 0),
                FossilTile(3, 1),
                FossilTile(3, 3),
                FossilTile(4, 2)
            )
        ),
        FossilMutation.allValues
    ),
    WEBBED(
        10, "10%",
        FossilShape(
            listOf(
                FossilTile(0, 2),
                FossilTile(1, 1),
                FossilTile(2, 0),
                FossilTile(3, 0),
                FossilTile(3, 1),
                FossilTile(3, 2),
                FossilTile(3, 3),
                FossilTile(4, 0),
                FossilTile(5, 1),
                FossilTile(6, 2),
            )
        ),
        listOf(
            FossilMutation.ROTATE_0,
            FossilMutation.FLIP_ROTATE_0,
        )
    ),
    CLUB(
        11, "9.1%",
        FossilShape(
            listOf(
                FossilTile(0, 2),
                FossilTile(0, 3),
                FossilTile(1, 2),
                FossilTile(1, 3),
                FossilTile(2, 1),
                FossilTile(3, 0),
                FossilTile(4, 0),
                FossilTile(5, 0),
                FossilTile(6, 0),
                FossilTile(6, 2),
                FossilTile(7, 1),
            )
        ),
        listOf(
            FossilMutation.ROTATE_0,
            FossilMutation.ROTATE_180,
            FossilMutation.FLIP_ROTATE_0,
            FossilMutation.FLIP_ROTATE_180,
        )
    ),
    SPINE(
        12, "8.3%",
        FossilShape(
            listOf(
                FossilTile(0, 2),
                FossilTile(1, 1),
                FossilTile(1, 2),
                FossilTile(2, 0),
                FossilTile(2, 1),
                FossilTile(2, 2),
                FossilTile(3, 0),
                FossilTile(3, 1),
                FossilTile(3, 2),
                FossilTile(4, 1),
                FossilTile(4, 2),
                FossilTile(5, 2),
            )
        ),
        FossilMutation.onlyRotation
    ),
    CLAW(
        13, "7.7%",
        FossilShape(
            listOf(
                FossilTile(0, 3),
                FossilTile(1, 2),
                FossilTile(1, 4),
                FossilTile(2, 1),
                FossilTile(2, 3),
                FossilTile(3, 1),
                FossilTile(3, 2),
                FossilTile(3, 4),
                FossilTile(4, 0),
                FossilTile(4, 1),
                FossilTile(4, 2),
                FossilTile(4, 3),
                FossilTile(5, 1),
            )
        ),
        FossilMutation.allValues
    ),
    FOOTPRINT(
        13, "7.7%",
        FossilShape(
            listOf(
                FossilTile(0, 2),
                FossilTile(1, 1),
                FossilTile(1, 2),
                FossilTile(1, 3),
                FossilTile(2, 1),
                FossilTile(2, 2),
                FossilTile(2, 3),
                FossilTile(3, 0),
                FossilTile(3, 2),
                FossilTile(3, 4),
                FossilTile(4, 0),
                FossilTile(4, 2),
                FossilTile(4, 4),
            )
        ),
        FossilMutation.onlyRotation
    ),
    HELIX(
        14, "7.1%",
        FossilShape(
            listOf(
                FossilTile(0, 0),
                FossilTile(0, 1),
                FossilTile(0, 2),
                FossilTile(0, 4),
                FossilTile(1, 0),
                FossilTile(1, 2),
                FossilTile(1, 4),
                FossilTile(2, 0),
                FossilTile(2, 4),
                FossilTile(3, 0),
                FossilTile(3, 1),
                FossilTile(3, 2),
                FossilTile(3, 3),
                FossilTile(3, 4),
            )
        ),
        FossilMutation.allValues
    ),
    UGLY(
        16, "6.2%",
        FossilShape(
            listOf(
                FossilTile(0, 1),
                FossilTile(1, 0),
                FossilTile(1, 1),
                FossilTile(1, 2),
                FossilTile(2, 0),
                FossilTile(2, 1),
                FossilTile(2, 2),
                FossilTile(2, 3),
                FossilTile(3, 0),
                FossilTile(3, 1),
                FossilTile(3, 2),
                FossilTile(3, 3),
                FossilTile(4, 0),
                FossilTile(4, 1),
                FossilTile(4, 2),
                FossilTile(5, 1),
            )
        ),
        FossilMutation.onlyRotation
    ),
    ;

    companion object {
        fun getByPercentage(percentage: String): MutableList<FossilType> {
            return entries.filter { it.firstPercentage == percentage }.toMutableList()
        }
    }
}

