package sperta.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sperta.domain.core.Home;
import sperta.domain.core.IUser;

public enum Repository {
    INSTANCE;
    
    Map<String, IUser> userCatalog = new ConcurrentHashMap<>(); //Ins....

    Map<Long, Home> homeCatalog = new ConcurrentHashMap<>(); //Instead of having a HomeCatalog class, let's just have this (Guilherme)
}
