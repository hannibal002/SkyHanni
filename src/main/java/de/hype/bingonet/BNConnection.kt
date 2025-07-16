package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig.BNPacketManager
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.environment.packetconfig.PacketUtils
import de.hype.bingonet.shared.constants.*
import de.hype.bingonet.shared.objects.*
import de.hype.bingonet.shared.packets.function.*
import de.hype.bingonet.shared.packets.function.MinionDataResponse.RequestMinionDataPacket
import de.hype.bingonet.shared.packets.network.*
import de.hype.bingonet.shared.packets.network.WantedSearchPacket.WantedSearchPacketReply
import tv.twitch.chat.Chat
import java.io.*
import java.lang.String
import java.math.BigInteger
import java.net.Socket
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collectors
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.collections.contains
import kotlin.collections.map
import kotlin.map
import kotlin.math.min
import kotlin.sequences.map
import kotlin.text.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("SkyHanniModuleInspection")
object BNConnection {
    var messageReceiverThread: Thread? = null
    var messageSenderThread: Thread? = null

    @JvmField
    var packetIntercepts: MutableList<BNInterceptPacketInfo<*>> = ArrayList()
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private lateinit var messageQueue: LinkedBlockingQueue<String>
    private var BNPacketManager: BNPacketManager
    var authenticated: Boolean? = null
        private set

    //Viewing Packet Traffic can pose as a Unfair Advantage (Splashes).
    val roles = mutableSetOf<BNRole>(BNRole.DEBUG)

    init {
        BNPacketManager = BNPacketManager(this)
    }

    private val config get() = SkyHanniMod.feature.event.bingo.bingoNet

    val waypoints: MutableList<WaypointData> = ArrayList()


    private fun createSSLContext(): SSLContext {
        // Load the certificate from resources/assets/public_bingonet_cert.crt
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certStream: InputStream? =
            javaClass.classLoader.getResourceAsStream("bingonet/public_bingonet_cert.crt")
        requireNotNull(certStream) { "Certificate file not found" }
        val certificate = certificateFactory.generateCertificate(certStream)
        certStream.close()

        // Create a KeyStore containing our certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("bingonet", certificate)

        // Initialize TrustManagerFactory with the KeyStore
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        // Create the SSLContext using the trust managers from our certificate
        return SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, SecureRandom())
        }
    }

    fun connect(serverIP: kotlin.String, serverPort: Int) {
        try {
            val sslContext = createSSLContext()
            val sslSocketFactory = sslContext.socketFactory

            socket = sslSocketFactory.createSocket(serverIP, serverPort).also { socket ->
                socket.soTimeout = 0
                socket.tcpNoDelay = true
                socket.keepAlive = true
            }

            messageQueue = LinkedBlockingQueue()
            reader = BufferedReader(InputStreamReader(socket?.inputStream!!))
            writer = PrintWriter(OutputStreamWriter(socket?.outputStream!!), true)

            setupMessageThreads()
        } catch (e: IOException) {
            throw RuntimeException("Failed to establish connection", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("SSL initialization failed", e)
        }
    }

    private fun setupMessageThreads() {
        messageReceiverThread = Thread(
            {
                try {
                    while (!Thread.currentThread().isInterrupted && isConnected) {
                        reader?.readLine()?.let { message ->
                            if (message.isNotEmpty()) {
                                onMessageReceived(message)
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    close()
                }
            },
            "BBsential-Receiver",
        ).apply {
            isDaemon = true
            start()
        }

        messageSenderThread = Thread(
            {
                try {
                    while (!Thread.currentThread().isInterrupted && isConnected) {
                        messageQueue.poll(100, TimeUnit.MILLISECONDS)?.let { message ->
                            writer?.println(message)
                            writer?.flush()
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            },
            "BBsential-Sender",
        ).apply {
            isDaemon = true
            start()
        }
    }


    fun onMessageReceived(message: kotlin.String) {
        if (!PacketUtils.handleIfPacket<AbstractPacket>(this, message.toString())) {
            if (message.startsWith("H-")) {
            } else {
                Chat.sendPrivateMessageToSelfSuccess("BB: $message")
            }
        }
    }

    fun <T : AbstractPacket> dummy(o: T?) {
        //this does absolutely nothing. dummy for packet in packt manager
    }

    fun <E : AbstractPacket> sendPacket(packet: E, blockLog: Boolean = false, retry: Int = 1) {
        val packetName = packet.javaClass.getSimpleName()
        val rawjson = PacketUtils.parsePacketToJson(packet)
        if (this.isConnected && writer != null) {
            if (!blockLog) {
                ChatUtils.clickableChat(
                    "[BN-Send]: $rawjson",
                    {
                        OSUtils.copyToClipboard(rawjson)
                    },
                    prefix = false,
                )
            }
            writer!!.println("$packetName.$rawjson")
        } else {
            if (retry <= 0) {
                ChatUtils.chat("§cBN: Failed to send packet $packetName. Not connected to Bingo Net Server.")
            } else {
                BNConnection.reconnectToBNserver()
                sendPacket(packet, blockLog, retry - 1)
            }
        }
    }

    fun onBroadcastMessagePacket(packet: BroadcastMessagePacket) {
        ChatUtils.chat("§6[BN-Announcement] §r[" + packet.prefix + "§r]§6 " + packet.username + ": " + packet.message, prefix = false)
    }

    fun onSplashNotifyPacket(packet: SplashNotifyPacket) {
        //influencing the delay in any way is disallowed!
        val waitTime: Int
        if (packet.splash.announcer == PlayerUtils.getName() && config.autoSplashStatusUpdates) {
            ChatUtils.chat("The Splash Update Statuses will be updatet automatically for you. If you need to do something manually go into Discord Splash Dashboard")
        } else {
            SplashManager.addSplash(packet.splash)
            if (packet.splash.lessWaste) {
                waitTime = min(((EnvironmentCore.utils.getPotTime() * 1000) / 80), 25 * 1000)
            } else {
                waitTime = 0
            }
            DelayedRun.runDelayed(waitTime.milliseconds) {
                SplashManager.display(packet.splash.splashId, SplashManager.SplashSource.BN)
            }
        }
    }

    fun onBingoChatMessagePacket(packet: BingoChatMessagePacket) {
        if (config.showBingoChat) {
            val prefix = if (packet.prefix == null) "" else "[${packet.prefix}§r]"
            ChatUtils.hoverableChat(
                "§6BC > §r$prefix ${packet.username}: ${packet.message}",
                listOf("Bingo Cards: ${packet.bingo_cards}"),
                prefix = false,
            )
        }
    }


    fun onWelcomePacket(packet: WelcomeClientPacket) {
        authenticated = packet.success
        if (packet.success) {
            roles.clear()
            roles.addAll(packet.roles)
            ChatUtils.chat("§aBN: Login Success")
        } else {
            ChatUtils.chat("§cBN: Login Failed")
        }
    }

    fun onDisconnectPacket(packet: DisconnectPacket) {
        for (i in packet.waitBeforeReconnect!!.indices) {
            val finalI = i
            DelayedRun.runDelayed(
                (packet.waitBeforeReconnect[i] + (Math.random() * packet.randomExtraDelay)).seconds,
                {
                    if (finalI == 0) {
                        connectToBBserver()
                    } else {
                        conditionalReconnectToBBserver()
                    }
                },
            )
        }

        val reason = packet.internalReason
        if (reason == InternalReasonConstants.NOT_REGISTERED) {
            ChatUtils.clickableLinkChat(
                "§cBN: You are not registered in the Bingo Net Network. Click here to view more Info.",
                "https://hackthetime.de/mod-not-registered",
            )
        } else if (reason == InternalReasonConstants.BANNED) {
            ChatUtils.chat("§cIt appears that you have been banned from the Bingo Net Network. Due to this the Bingo Net Integration deactivated itself!")
            config.useBN = false
        } else if (packet.waitBeforeReconnect?.isEmpty() ?: true) {
            ChatUtils.chat("§cBN: You have been disconnected from the Bingo Net Network.")
        } else {
            for (i in packet.waitBeforeReconnect) {
                DelayedRun.runDelayed(
                    i.seconds,
                    {
                        conditionalReconnectToBBserver()
                    },
                )
            }
        }
    }

    fun onInvalidCommandFeedbackPacket(packet: InvalidCommandFeedbackPacket) {
        //TODO upgrade via sth like run command packet interface and then reply just copmmand failed maybe error too or sth and then fail command execution and show user exact command or sth? maybe clickable for slighly changeable?
        ChatUtils.chat("§cBN: ${packet.displayMessage}")
    }

    fun onPartyPacket(packet: PartyPacket) {
        if (config.allow_bn_server_party) {
            val isInParty = PartyApi.isInParty()
            if (!isInParty && !(packet.type == PartyConstants.JOIN || packet.type == PartyConstants.ACCEPT || packet.type == PartyConstants.INVITE)) return
            val leader = PartyApi.isPartyLeader()
            val moderator = PartyApi.isModerator()

            if (packet.type == PartyConstants.JOIN) {
                PartyApi.leaveParty()
                ChatUtils.clickableChat(
                    "BN: Joining party requested by Bingo Net Server. Click to disable this Permission!",
                    {
                        config.allow_bn_server_party = false
                    },
                )
                PartyApi.joinParty(packet.users.first())
            } else if (packet.type == PartyConstants.ACCEPT) {
                PartyApi.leaveParty()
                ChatUtils.clickableChat(
                    "BN: Joining party requested by Bingo Net Server. Click to disable this Permission!",
                    {
                        config.allow_bn_server_party = false
                    },
                )
                PartyApi.acceptParty(packet.users.first())
            } else if (packet.type == PartyConstants.DISBAND) {
                ChatUtils.clickableChat(
                    "BN: Party Disband requested by Bingo Net Server. Click to disable this Permission!",
                    {
                        config.allow_bn_server_party = false
                    },
                )
                if (!PartyApi.disband()) {
                    HypixelCommands.partyChat("Bingo Net Server requested party disband but you are not the leader. Leaving party")
                }
            } else if (packet.type == PartyConstants.INVITE) {
                if (PartyApi.canInvite()) {
                    PartyApi.invite(packet.users)
                } else {
                    HypixelCommands.partyChat("/pc Bingo Net Server requested a party invite for: ${packet.users}")
                }
            } else if (packet.type == PartyConstants.WARP) {
                if (PartyApi.warp()) {
                    ChatUtils.clickableChat(
                        "BN: Party Disband requested by Bingo Net Server. Click to disable this Permission!",
                        {
                            config.allow_bn_server_party = false
                        },
                    )
                }
            } else if (packet.type == PartyConstants.KICK) {
                if (PartyApi.kick(packet.users)) {
                    ChatUtils.clickableChat(
                        "BN: Party kicks requested by Bingo Net Server. Click to disable this Permission!",
                        {
                            config.allow_bn_server_party = false
                        },
                    )
                }
            } else if (packet.type == PartyConstants.PROMOTE) {
                PartyApi.promote(packet.users.first())
            } else if (packet.type == PartyConstants.LEAVE) {
                PartyApi.leaveParty()
            }
        }
    }

    fun onSystemMessagePacket(packet: SystemMessagePacket) {
        if (packet.important) {
            ChatUtils.chat("§n${packet.message}")
        } else {
            ChatUtils.chat(packet.message)
        }
        if (packet.ping) {
            SoundUtils.playPlingSound()
        }
    }

    fun onRequestAuthentication(packet: RequestAuthentication) {
        if (socket!!.getPort() == 5011) {
            ChatUtils.chat("§aBN: Logging into Bingo Net (§6Beta§a)")
            ChatUtils.chat("§6You may test here but do NOT Spam unless you have very good reasons. Spamming may still be punished")
        } else {
            ChatUtils.chat("§aBN: Logging into Bingo Net")
        }
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        val r1 = Random()
        val r2 = Random(System.identityHashCode(Any()).toLong())
        val random1Bi = BigInteger(64, r1)
        val random2Bi = BigInteger(64, r2)
        val serverBi = random1Bi.xor(random2Bi)
        val clientRandom = serverBi.toString(16)

        val serverId = clientRandom + packet.serverIdSuffix

        if (config.bn_api_key.isEmpty()) {
            MojangUtils.authServer(serverId)
            val connectPacket = RequestConnectPacket(
                PlayerUtils.getRawUuid(),
                clientRandom,
                EnvironmentCore.utils.getGameVersion(),
                EnvironmentCore.utils.getModVersion(),
                "SkyHanni",
                AuthenticationConstants.MOJANG,
            )
            sendPacket(connectPacket)
        } else {
            sendPacket(
                RequestConnectPacket(
                    PlayerUtils.getRawUuid(),
                    config.bn_api_key,
                    EnvironmentCore.utils.getGameVersion(),
                    EnvironmentCore.utils.getModVersion(),
                    "SkyHanni",
                    AuthenticationConstants.DATABASE,
                ),
            )
        }
    }


    val isConnected: Boolean
        get() {
            try {
                return socket!!.isConnected() && !socket!!.isClosed()
            } catch (e: Exception) {
                return false
            }
        }

    fun close() {
        try {
            if (messageReceiverThread != null) {
                messageReceiverThread!!.interrupt()
            }
            if (messageSenderThread != null) {
                messageSenderThread!!.interrupt()
            }
            if (BingoNet.bbthread != null) {
                BingoNet.bbthread.interrupt()
            }
            writer?.close()
            reader?.close()
            messageQueue?.clear()
            if (BingoNet.bbthread != null) {
                BingoNet.bbthread.interrupt()
                BingoNet.bbthread = null
            }
            if (messageSenderThread != null) {
                messageSenderThread!!.interrupt()
                messageSenderThread = null
            }
            if (messageReceiverThread != null) {
                messageReceiverThread!!.interrupt()
                messageReceiverThread = null
            }
            writer = null
            reader = null
            socket = null
        } catch (e: Exception) {
            if (e.message != null) Chat.sendPrivateMessageToSelfError(e.message)
            e.printStackTrace()
        }
    }

    fun onWaypointPacket(packet: WaypointPacket) {
        if (packet.operation == WaypointPacket.Operation.ADD) {
            Waypoints(packet.waypoint)
        } else if (packet.operation == WaypointPacket.Operation.REMOVE) {
            try {
                Waypoints.waypoints.get(packet.waypointId)!!.removeFromPool()
            } catch (ignored: Exception) {
            }
        } else if (packet.operation == WaypointPacket.Operation.EDIT) {
            try {
                val oldWaypoint: Waypoints = Waypoints.waypoints.get(packet.waypointId)!!
                oldWaypoint.replaceWithNewWaypoint(packet.waypoint, packet.waypointId)
            } catch (ignored: Exception) {
            }
        }
    }

    fun onGetWaypointsPacket(packet: GetWaypointsPacket) {
        sendPacket(
            GetWaypointsPacket(
                Waypoints.waypoints.values
                    .map<ClientWaypointData?>((Function { waypoint: Waypoints? -> (waypoint as ClientWaypointData?) }))
                    .collect(
                        Collectors.toList(),
                    ),
            ),
        )
    }

    fun onCompletedGoalPacket(packet: CompletedGoalPacket) {
        if (!config.showCardCompletions && packet.completionType == CompletedGoalPacket.CompletionType.CARD) Chat.sendPrivateMessageToSelfText(
            ChatUtils.hoverableChat("§6${packet.username}§7 just completed the Bingo!", packet.lore.split("\n")),
        )
        else if (!config.showGoalCompletions && packet.completionType == CompletedGoalPacket.CompletionType.GOAL) Chat.sendPrivateMessageToSelfText(
            ChatUtils.hoverableChat("§6${packet.username}§7 just completed the Goal §6${packet.name}§7!", packet.lore.split("\n")),
        )
    }

    fun onPlaySoundPacket(packet: PlaySoundPacket) {
        if (!packet.isStreamFromUrl) createSound(packet.soundId, 1F).playSound()
    }

    fun onWantedSearchPacket(packet: WantedSearchPacket) {

        if (packet.serverId != null && !(HypixelData.serverId?.matches(Regex(packet.serverId)) ?: false)
        ) return
        if (packet.mega != null && packet.mega != HypixelData.isInMega()) return
        val players: List<String> = HypixelData.getPlayersOnCurrentServer()
        if (packet.maximumPlayerCount != null && packet.maximumPlayerCount <= players.size) return
        if (packet.minimumPlayerCount != null && packet.minimumPlayerCount >= players.size) return
        if (packet.username != null && !players.contains(packet.username)) return
        sendPacket(
            packet.preparePacketToReplyToThis(
                WantedSearchPacketReply(
                    PlayerUtils.getName(),
                    players,
                    HypixelData.isInMega(),
                    HypixelData.serverId ?: "Error",
                ),
            ),
        )
    }

    fun onPunishedPacket(data: PunishedPacket) {
        if (data.disconnectFromNetworkOnLoad) close()
        if (data.modSelfRemove) selfDestruct()
        if (!data.silentCrash) {
            Chat.sendPrivateMessageToSelfFatal("You have been ${data.type}ed in the Bingo Net Network!")
            if (data.modSelfRemove) Chat.sendPrivateMessageToSelfFatal("You are no longer Permitted to use the Mod. The Mod will now automatically Remove itself.")
        }
        if (data.shouldModCrash) {
            for (i in 0..<data.warningTimeBeforeCrash) {
                if (!data.silentCrash) Chat.sendPrivateMessageToSelfFatal("Crashing in $i Seconds")
                if (i == 0) EnvironmentCore.utils.systemExit(data.exitCodeOnCrash)
            }
        }
    }

    fun onRequestMinionDataPacket(packet: RequestMinionDataPacket) {
        sendPacket(packet.preparePacketToReplyToThis(EnvironmentCore.utils.getMiniondata()))
    }

    fun onCommandChatPromptPacket(packet: CommandChatPromptPacket) {
        val prompt = ChatPrompt(
            Runnable {
                for (command in packet.getCommands()) {
                    BingoNet.sender.addSendTask(command.command, command.delay)
                }
            },
            10,
        )
        Chat.sendPrivateMessageToSelfText(packet.printMessage)
        BingoNet.temporaryConfig.lastChatPromptAnswer = prompt
    }

    fun onPacketChatPromptPacket(packet: PacketChatPromptPacket) {
        val prompt = ChatPrompt(
            Runnable {
                for (p in packet.packets!!) {
                    sendPacket(p)
                }
            },
            10,
        )
        Chat.sendPrivateMessageToSelfText(packet.printMessage)
        BingoNet.temporaryConfig.lastChatPromptAnswer = prompt
    }

    @HandleEvent
    fun rendering(event: SkyHanniRenderWorldEvent) {
        for (data in waypoints.filter { it.visible }) {
            val position = data.position.toLorenz()
            val distance = position.distanceToPlayer()
            if (distance > data.renderDistance) continue

            event.drawWaypointFilled(
                position,
                data.color,
                seeThroughBlocks = data.renderThroughBlocks,
                beacon = data.renderBeacon,
            )
            event.drawDynamicText(position,"§6[BN]-${data.text}",1.0)
        }
    }
}

private fun Position.toLorenz(): LorenzVec {
    return LorenzVec(
        x.toDouble(),
        y.toDouble(),
        z.toDouble(),
    )
}
