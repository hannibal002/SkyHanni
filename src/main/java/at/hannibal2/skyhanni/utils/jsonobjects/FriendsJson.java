package at.hannibal2.skyhanni.utils.jsonobjects;


import java.util.Map;
import java.util.UUID;

public class FriendsJson {

    public Map<UUID, PlayerFriends> players;

    public static class PlayerFriends {

        public Map<UUID, Friend> friends;

        public static class Friend {
            public String name;
            public boolean bestFriend;
        }
    }
}
