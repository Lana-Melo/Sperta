package sperta.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sperta.domain.core.Home;
import sperta.domain.core.IUser;

public enum Repository {
    INSTANCE;
    
    Map<String, IUser> UserCatalog = new ConcurrentHashMap<>();

    Map<Long, Home> HomeCatalog = new ConcurrentHashMap<>();
}
