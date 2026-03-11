package sperta.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.utils.Persistence;

public enum Repository {
    INSTANCE;
    
    Map<String, IUser> userCatalog = new ConcurrentHashMap<>();
    Map<Long, House> homeCatalog = new ConcurrentHashMap<>();

    private Repository() {
        Persistence.load("./data/users.txt", userCatalog);
        Persistence.load("./data/homes.txt", homeCatalog);
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

    public boolean updateUser(IUser user) {
        if (!userCatalog.containsKey(user.getUserId())) {
            return false;
        }
        userCatalog.put(user.getUserId(), user);
        return true;
    }

    public boolean removeUser(String userId) {
        if (!userCatalog.containsKey(userId)) {
            return false;
        }
        userCatalog.remove(userId);
        return true;
    }

    // ==================================================
    // CRUD operations for houses
    // ==================================================
    public boolean addHouse(House house) {
        if (homeCatalog.containsKey(house.getId())) {
            return false;
        }
        homeCatalog.put(house.getId(), house);
        return true;
    }

    public House getHouseById(Long houseId) {
        if (!homeCatalog.containsKey(houseId)) {
            return null;
        }
        return homeCatalog.get(houseId);
    }

    public boolean updateHouse(House house) {
        if (!homeCatalog.containsKey(house.getId())) {
            return false;
        }
        homeCatalog.put(house.getId(), house);
        return true;
    }

    public boolean removeHouse(Long houseId) {
        if (!homeCatalog.containsKey(houseId)) {
            return false;
        }
        homeCatalog.remove(houseId);
        return true;
    }
}
