package sperta.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sperta.domain.core.House;
import sperta.domain.core.IUser;

public enum Repository {
    INSTANCE;
    
    Map<String, IUser> userCatalog = new ConcurrentHashMap<>(); //Ins....

    Map<Long, House> homeCatalog = new ConcurrentHashMap<>(); //Instead of having a HomeCatalog class, let's just have this (Guilherme)

    //I think that methods in this Repository should all be static!
}
