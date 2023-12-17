package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.data.Powers;
import at.hannibal2.skyhanni.data.model.ComposterUpgrade;
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal;
import at.hannibal2.skyhanni.features.combat.endernodetracker.EnderNodeTracker;
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostData;
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI;
import at.hannibal2.skyhanni.features.event.diana.DianaProfitTracker;
import at.hannibal2.skyhanni.features.event.diana.MythologicalMobTracker;
import at.hannibal2.skyhanni.features.event.jerry.frozentreasure.FrozenTreasureTracker;
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker;
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity;
import at.hannibal2.skyhanni.features.garden.CropAccessory;
import at.hannibal2.skyhanni.features.garden.CropType;
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI;
import at.hannibal2.skyhanni.features.garden.farming.ArmorDropTracker;
import at.hannibal2.skyhanni.features.garden.farming.DicerDropTracker;
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems;
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward;
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderTracker;
import at.hannibal2.skyhanni.features.misc.trevor.TrevorTracker;
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWord;
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminTracker;
import at.hannibal2.skyhanni.features.rift.area.westvillage.kloon.KloonTerminal;
import at.hannibal2.skyhanni.features.slayer.SlayerProfitTracker;
import at.hannibal2.skyhanni.utils.LorenzVec;
import at.hannibal2.skyhanni.utils.NEUInternalName;
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker;
import com.google.gson.annotations.Expose;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Storage {

    @Expose
    public boolean hasPlayedBefore = false;

    @Expose
    public Float savedMouseSensitivity = .5f;

    @Deprecated
    @Expose
    public Map<String, List<String>> knownFeatureToggles = new HashMap<>();

    @Expose
    public List<VisualWord> modifiedWords = new ArrayList<>();

    @Expose
    public boolean visualWordsImported = false;

    @Expose
    public Boolean contestSendingAsked = false;

    @Expose
    public Map<String, SkyHanniTracker.DisplayMode> trackerDisplayModes = new HashMap<>();

    @Expose
    public Map<UUID, PlayerSpecific> players = new HashMap<>();

    public static class PlayerSpecific {

        @Expose
        public Map<String, ProfileSpecific> profiles = new HashMap<>(); // profile name

        @Expose
        public Integer gardenCommunityUpgrade = -1;

        @Expose
        public long nextCityProjectParticipationTime = 0L;

        @Expose
        public String currentAccountUpgrade = null;

        @Expose
        public long nextAccountUpgradeCompletionTime = -1L;

        @Expose
        public List<String> guildMembers = new ArrayList<>();

        @Expose
        public WinterStorage winter = new WinterStorage();

        public static class WinterStorage {

            @Expose
            public Set<String> playersThatHaveBeenGifted = new HashSet<>();

            @Expose
            public int amountGifted = 0;

            @Expose
            public int cakeCollectedYear = 0;
        }

        @Expose
        public Map<Long, BingoSession> bingoSessions = new HashMap<>();

        public static class BingoSession {

            @Expose
            public List<String> tierOneMinionsDone = new ArrayList<>();

            @Expose
            public Map<Integer, BingoGoal> goals = new HashMap<>();
        }
    }

    public static class ProfileSpecific {

        @Expose
        public String currentPet = "";

        @Expose
        public Powers currentPower = null;

        @Expose
        public Map<LorenzVec, MinionConfig> minions = new HashMap<>();

        public static class MinionConfig {

            @Expose
            public String displayName = "";

            @Expose
            public long lastClicked = -1;

            @Override
            public String toString() {
                return "MinionConfig{" +
                        "displayName='" + displayName + '\'' +
                        ", lastClicked=" + lastClicked +
                        '}';
            }
        }

        @Expose
        public CrimsonIsleStorage crimsonIsle = new CrimsonIsleStorage();

        public static class CrimsonIsleStorage {

            @Expose
            public List<String> quests = new ArrayList<>();

            @Expose
            public List<String> miniBossesDoneToday = new ArrayList<>();

            @Expose
            public List<String> kuudraTiersDone = new ArrayList<>();

            @Expose
            public Map<String, Map<TrophyRarity, Integer>> trophyFishes = new HashMap<>();
        }

        @Expose
        public ProfileSpecific.GardenStorage garden = new ProfileSpecific.GardenStorage();

        public static class GardenStorage {

            @Expose
            public Long experience = null;

            @Expose
            public Map<CropType, Long> cropCounter = new HashMap<>();

            @Expose
            public Map<CropType, Integer> cropUpgrades = new HashMap<>();

            @Expose
            public Map<CropType, Integer> cropsPerSecond = new HashMap<>();

            @Expose
            public Map<CropType, Double> latestBlocksPerSecond = new HashMap<>();

            @Expose
            public Map<CropType, Double> latestTrueFarmingFortune = new HashMap<>();

            @Expose
            public CropAccessory savedCropAccessory = null;

            @Expose
            public DicerDropTracker.Data dicerDropTracker = new DicerDropTracker.Data();

            @Expose
            public long informedAboutLowMatter = 0;

            @Expose
            public long informedAboutLowFuel = 0;

            @Expose
            public long visitorInterval = 15 * 60_000L;

            @Expose
            public long nextSixthVisitorArrival = 0;

            @Expose
            public ArmorDropTracker.Data armorDropTracker = new ArmorDropTracker.Data();

            @Expose
            public Map<ComposterUpgrade, Integer> composterUpgrades = new HashMap<>();

            @Expose
            public Map<CropType, Boolean> toolWithBountiful = new HashMap<>();

            @Expose
            public String composterCurrentOrganicMatterItem = "";

            @Expose
            public String composterCurrentFuelItem = "";

            @Expose
            public int uniqueVisitors = 0;

            @Expose
            public VisitorDrops visitorDrops = new VisitorDrops();

            public static class VisitorDrops {
                @Expose
                public int acceptedVisitors = 0;

                @Expose
                public int deniedVisitors = 0;

                @Expose
                public List<Long> visitorRarities = new ArrayList<>();

                @Expose
                public int copper = 0;

                @Expose
                public long farmingExp = 0;

                @Expose
                public int gardenExp = 0;

                @Expose
                public long coinsSpent = 0;

                @Expose
                public long bits = 0;

                @Expose
                public long mithrilPowder = 0;

                @Expose
                public long gemstonePowder = 0;

                @Expose
                public Map<VisitorReward, Integer> rewardsCount = new HashMap<>();
            }

            @Expose
            public PlotIcon plotIcon = new PlotIcon();

            public static class PlotIcon {
                @Expose
                public Map<Integer, NEUInternalName> plotList = new HashMap<>();
            }

            @Expose
            public Map<Integer, GardenPlotAPI.PlotData> plotData = new HashMap<>();

            @Expose
            public Map<CropType, LorenzVec> cropStartLocations = new HashMap<>();

            @Expose
            public Fortune fortune = new Fortune();

            public static class Fortune {

                @Expose
                public Map<FarmingItems, Boolean> outdatedItems = new HashMap<>();

                @Expose
                public int anitaUpgrade = -1;

                @Expose
                public int farmingStrength = -1;

                @Expose
                public int farmingLevel = -1;

                @Expose
                public int plotsUnlocked = -1;

                @Expose
                public long cakeExpiring = -1L;

                @Expose
                public boolean carrotFortune = false;

                @Expose
                public boolean pumpkinFortune = false;

                @Expose
                public Map<FarmingItems, ItemStack> farmingItems = new HashMap<>();
            }

            @Expose
            public long composterEmptyTime = 0;

            @Expose
            public long lastComposterEmptyWarningTime = 0;

            @Expose
            public FarmingWeightConfig farmingWeight = new FarmingWeightConfig();

            public static class FarmingWeightConfig {

                @Expose
                public int lastFarmingWeightLeaderboard = -1;
            }
        }

        @Expose
        public GhostCounter ghostCounter = new GhostCounter();

        public static class GhostCounter {

            @Expose
            public Map<GhostData.Option, Double> data = new HashMap<>();

            @Expose
            public boolean ctDataImported = false;

            @Expose
            public double bestiaryNextLevel = 0;

            @Expose
            public double bestiaryCurrentKill = 0;

            @Expose
            public double bestiaryKillNeeded = 0;

            @Expose
            public double totalMF = 0;

            @Expose
            public int configUpdateVersion = 0;

        }

        @Expose
        public PowderTracker.Data powderTracker = new PowderTracker.Data();

        @Expose
        public FrozenTreasureTracker.Data frozenTreasureTracker = new FrozenTreasureTracker.Data();

        @Expose
        public EnderNodeTracker.Data enderNodeTracker = new EnderNodeTracker.Data();

        @Expose
        public RiftStorage rift = new RiftStorage();

        public static class RiftStorage {

            @Expose
            public List<KloonTerminal> completedKloonTerminals = new ArrayList<>();

            @Expose
            public VerminTracker.Data verminTracker = new VerminTracker.Data();

        }

        @Expose
        public Map<String, SlayerProfitTracker.Data> slayerProfitData = new HashMap<>();

        @Expose
        public Map<String, SlayerRngMeterStorage> slayerRngMeter = new HashMap<>();

        public static class SlayerRngMeterStorage {

            @Expose
            public long currentMeter = -1;

            @Expose
            public long gainPerBoss = -1;

            @Expose
            public long goalNeeded = -1;

            @Expose
            public String itemGoal = "?";

            @Override
            public String toString() {
                return "SlayerRngMeterStorage{" +
                        "currentMeter=" + currentMeter +
                        ", gainPerBoss=" + gainPerBoss +
                        ", goalNeeded=" + goalNeeded +
                        ", itemGoal='" + itemGoal + '\'' +
                        '}';
            }
        }

        @Expose
        public MiningConfig mining = new MiningConfig();

        public static class MiningConfig {

            @Expose
            public List<String> kingsTalkedTo = new ArrayList<>();
        }

        @Expose
        public TrapperData trapperData = new TrapperData();

        public static class TrapperData {

            @Expose
            public int questsDone;

            @Expose
            public int peltsGained;

            @Expose
            public int killedAnimals;

            @Expose
            public int selfKillingAnimals;

            @Expose
            public Map<TrevorTracker.TrapperMobRarity, Integer> animalRarities = new HashMap<>();
        }

        @Expose
        public DungeonStorage dungeons = new DungeonStorage();

        public static class DungeonStorage {

            @Expose
            public Map<DungeonAPI.DungeonFloor, Integer> bosses = new HashMap<>();
        }

        @Expose
        public FishingStorage fishing = new FishingStorage();

        public static class FishingStorage {

            @Expose
            public FishingProfitTracker.Data fishingProfitTracker = new FishingProfitTracker.Data();

        }

        @Expose
        public DianaStorage diana = new DianaStorage();

        public static class DianaStorage {

            @Expose
            // TODO rename to 'profitTracker'
            public DianaProfitTracker.Data dianaProfitTracker = new DianaProfitTracker.Data();

            @Expose
            public MythologicalMobTracker.Data mythologicalMobTracker = new MythologicalMobTracker.Data();

        }
    }
}
