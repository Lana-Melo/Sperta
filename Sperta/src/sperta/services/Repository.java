package sperta.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.utils.Persistence;

public enum Repository {
    INSTANCE;

    private static final String HOUSE_PATH = "./data/houses/";

    private Map<String, IUser> userCatalog = new ConcurrentHashMap<>();
    private Map<String, House> homeCatalog = new ConcurrentHashMap<>();

    private Object userLock = new Object();
    private Object homeLock = new Object();

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
        synchronized (userLock) {
            if (userCatalog.containsKey(user.getUserId())) {
                return false;
            }
            userCatalog.put(user.getUserId(), user);
        }
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
        synchronized (homeLock) {
            if (homeCatalog.containsKey(house.getId())) {
                //System.out.println("Já tem casa");
                return false;
            }
            homeCatalog.put(house.getId(), house);
        }

        try {
            // If creating the files wasn't possible, the operation fails
            Persistence.createLogFiles(
                HOUSE_PATH + "/" + house.getName(),
                house.getName()
            );
        } catch (IOException e) {
            homeCatalog.remove(house.getId());
            return false;
        }
        return true;
    }

    public House getHouseById(String houseId) {
        if (!homeCatalog.containsKey(houseId)) {
            return null;
        }
        return homeCatalog.get(houseId);
    }
}
