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

    


}
