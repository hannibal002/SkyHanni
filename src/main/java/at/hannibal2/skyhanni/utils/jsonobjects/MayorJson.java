package at.hannibal2.skyhanni.utils.jsonobjects;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

public class MayorJson {
    @Expose
    public boolean success;
    @Expose
    public long lastUpdated;
    @Expose
    public Mayor mayor;
    @Expose
    public Election current;

    public class Candidate {
        @Expose
        public String key;
        @Expose
        public String name;
        @Expose
        public ArrayList<Perk> perks;
        @Expose
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

    public class Election {
        @Expose
        public int year;
        @Expose
        public ArrayList<Candidate> candidates;
    }

    public class Mayor {
        @Expose
        public String key;
        @Expose
        public String name;
        @Expose
        public ArrayList<Perk> perks;
        @Expose
        public Election election;
    }

    public static class Perk {
        @Expose
        public String name;
        @Expose
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
