package sperta.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.utils.Persistence;

public enum Repository {
    INSTANCE;
    
    Map<String, IUser> userCatalog = new ConcurrentHashMap<>();
    Map<String, House> homeCatalog = new ConcurrentHashMap<>();

    private Repository() {
        Persistence.load("./data/users.txt", userCatalog);
        Persistence.load("./data/homes.txt", homeCatalog);
    }

    public void persist() {
        Persistence.persist("./data/users.txt", userCatalog);
        Persistence.persist("./data/homes.txt", homeCatalog); 
    }

    // ==================================================
    // CRUD operations for users
    // ==================================================
    public boolean addUser(IUser user) {
        if (userCatalog.containsKey(user.getUserId())) {
            return false;
        }
        userCatalog.put(user.getUserId(), user);
        return true;
    }

    public IUser getUserById(String userId) {
        if (!userCatalog.containsKey(userId)) {
            return null;
        }
        return userCatalog.get(userId);
    }

    // ==================================================
    // CRUD operations for houses
    // ==================================================
    public boolean addHouse(House house) {
        if (homeCatalog.containsKey(house.getId())) {
            return false;
        }
        homeCatalog.put(house.getName(), house);
        return true;
    }

    public House getHouseById(String houseId) {
        if (!homeCatalog.containsKey(houseId)) {
            return null;
        }
        return homeCatalog.get(houseId);
    }
}
