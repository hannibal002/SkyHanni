package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import org.junit.jupiter.api.Test

class VisitorApiTest {

    @Test
    fun testFromHypixelName() {
        assert(VisitorApi.fromHypixelName(" §r Jacob") == "§fJacob")
        assert(VisitorApi.fromHypixelName("§r Jacob") == "§fJacob")
        assert(VisitorApi.fromHypixelName("§rJacob") == "§fJacob")
        assert(VisitorApi.fromHypixelName("Jacob") == "§fJacob")
        assert(VisitorApi.fromHypixelName(" Jacob ") == "§fJacob")
        assert(VisitorApi.fromHypixelName("§cSpaceman") == "§cSpaceman")
        assert(VisitorApi.fromHypixelName("§cGrandma Wolf") == "§cGrandma Wolf")
    }

    @Test
    fun testIsVisitorInfo() {

        // To short
        assert(
            !VisitorApi.isVisitorInfo(
                mutableListOf(
                    "Visitor Info",
                    "Click to view info about this visitor."
                )
            )
        )

        // To long
        assert(
            !VisitorApi.isVisitorInfo(
                mutableListOf(
                    "Visitor Info",
                    "Click to view info about this visitor.",
                    "Click to view info about this visitor.",
                    "Click to view info about this visitor.",
                    "Click to view info about this visitor.",
                )
            )
        )

        // Third line is Offers Accepted:
        assert(
            VisitorApi.isVisitorInfo(
                mutableListOf(
                    "Visitor Info",
                    "Click to view info about this visitor.",
                    "Click to view info about this visitor.",
                    "Offers Accepted: 54",
                )
            )
        )
    }
}
