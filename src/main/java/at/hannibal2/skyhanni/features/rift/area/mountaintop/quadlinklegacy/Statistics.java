package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy;

import java.util.HashSet;
import java.util.Set;

final class Statistics {

    private long exploredNodes = 0;
    private long ttHits = 0;
    private long ttMisses = 0;
    private long ttCacheEvictions = 0;
    private long ttSets = 0;
    private long ttGets = 0;
    private long ttOptimalHits = 0;

    private final Set<Long> ttSeenKeys = Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL ? new HashSet<>() : null;

    void incrementExploredNodes() {

        if (Config.STATISTICS_ENABLED) {

            ++exploredNodes;
        }
    }

    void incrementTTHits() {

        if (Config.STATISTICS_ENABLED) {

            ++ttHits;
        }
    }

    void incrementTTMisses() {

        if (Config.STATISTICS_ENABLED) {

            ++ttMisses;
        }
    }

    void incrementTTCacheEvictions() {

        if (Config.STATISTICS_ENABLED) {

            ++ttCacheEvictions;
        }
    }

    long getExploredNodes() {

        return Config.STATISTICS_ENABLED ? exploredNodes : 0;
    }

    long getTTHits() {

        return Config.STATISTICS_ENABLED ? ttHits : 0;
    }

    long getTTMisses() {

        return Config.STATISTICS_ENABLED ? ttMisses : 0;
    }

    void recordTTSet(long key) {

        if (Config.STATISTICS_ENABLED) {

            ++ttSets;
            if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL) {

                ttSeenKeys.add(key);
            }
        }
    }

    void recordTTGet(long key) {

        if (Config.STATISTICS_ENABLED) {

            ++ttGets;
            if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL && ttSeenKeys.contains(key)) {

                ++ttOptimalHits;
            }
        }
    }

    long getTTCacheEvictions() {

        return Config.STATISTICS_ENABLED ? ttCacheEvictions : 0;
    }

    long getTTSets() {

        return Config.STATISTICS_ENABLED ? ttSets : 0;
    }

    long getTTGets() {

        return Config.STATISTICS_ENABLED ? ttGets : 0;
    }

    long getTTOptimalHits() {

        return Config.STATISTICS_ENABLED ? ttOptimalHits : 0;
    }

    void reset() {

        if (Config.STATISTICS_ENABLED) {

            exploredNodes = 0;
            ttHits = 0;
            ttMisses = 0;
            ttCacheEvictions = 0;
            ttSets = 0;
            ttGets = 0;
            ttOptimalHits = 0;
            if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL) {

                ttSeenKeys.clear();
            }
        }
    }
}
