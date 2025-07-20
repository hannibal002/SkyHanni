package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class TransactionConfig {

    @Expose
    @ConfigOption(name = "AH Messages", desc = "Hide messages about setting up, processing and claiming auctions.")
    @SearchTag("BIN")
    @ConfigEditorBoolean
    var uselessAH: Boolean = false

    @Expose
    @ConfigOption(name = "Allowance", desc = "Hides 'ALLOWANCE! You earned #' messages.")
    @ConfigEditorBoolean
    var allowance: Boolean = false

    @Expose
    @ConfigOption(name = "Auction Claim", desc = "Hide 'Visit the Auction House to collect your item!' messages.")
    @ConfigEditorBoolean
    var auctionHouseClaim: Boolean = false

    @Expose
    @ConfigOption(name = "Bank Messages", desc = "Hide 'Depositing coins...' and 'Withdrawing coins...' messages.")
    @ConfigEditorBoolean
    var bankDepositWithdraw: Boolean = false

    @Expose
    @ConfigOption(name = "Bazaar Cancel", desc = "Hide 'Cancelling order...' message.")
    @ConfigEditorBoolean
    var bazaarCancel: Boolean = false

    @Expose
    @ConfigOption(name = "Bazaar Claim", desc = "Hide 'Claiming order...' messages.")
    @ConfigEditorBoolean
    var bazaarClaim: Boolean = false

    @Expose
    @ConfigOption(name = "Bazaar Escrow", desc = "Hide 'Putting coins in escrow...' messages")
    @ConfigEditorBoolean
    var bazaarEscrow: Boolean = false

    @Expose
    @ConfigOption(name = "Bazaar Instant", desc = "Hide 'Executing instant buy/sell...' messages.")
    @ConfigEditorBoolean
    var bazaarInstant: Boolean = false

    @Expose
    @ConfigOption(name = "Bazaar Submitting Orders", desc = "Hide buy/sell 'Submitting...c' order messages.")
    @ConfigEditorBoolean
    var bazaarOrder: Boolean = false
}
