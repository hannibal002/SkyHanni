package at.hannibal2.skyhanni.utils.jsonobjects;


import java.util.ArrayList;

public class MayorJson {
    public boolean success;
    public long lastUpdated;
    public Mayor mayor;
    public Election current;

    public class Candidate {
        public String key;
        public String name;
        public ArrayList<Perk> perks;
        public int votes;

        @Override
        public String toString() {
            return "Candidate{" +
                    "key='" + key + '\'' +
                    ", name='" + name + '\'' +
                    ", perks=" + perks +
                    ", votes=" + votes +
                    '}';
        }
    }

//    public class Current {
//        public int year;
//        public ArrayList<Candidate> candidates;
//    }

    public class Election {
        public int year;
        public ArrayList<Candidate> candidates;
    }

    public class Mayor {
        public String key;
        public String name;
        public ArrayList<Perk> perks;
        public Election election;
    }

    public static class Perk {
        public String name;
        public String description;

        @Override
        public String toString() {
            return "Perk{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}