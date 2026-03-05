public enum Repository {
    INSTANCE
    
    Map<String, IUser> UserCatalog = ConcurrentHashMap<>();
    Map<long, Home> HomeCatalog = ConcurrentHashMap<>();
}
