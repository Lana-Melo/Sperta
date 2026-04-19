package sperta.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.utils.Persistence;

/**
 * Singleton repository for users and houses.
 * Provides thread-safe CRUD operations and persistence.
 */
public enum Repository {
    INSTANCE;

    private static final String HOUSES_PATH = "./SpertaServer/data/houses/";
    private static final String USER_CERTS_PATH = "./SpertaServer/data/user_certificates";

    private Map<String, IUser> userCatalog = new ConcurrentHashMap<>();
    private Map<String, House> homeCatalog = new ConcurrentHashMap<>();

    private Object userLock = new Object();
    private Object homeLock = new Object();

    /**
     * Private constructor to prevent instantiation.
     * Initialization is explicit and handled by the application layer.
     */
    private Repository() {}

    /**
     * Loads repository data from persistence files.
     * Caller is responsible for handling startup I/O failures.
     */
    public void initialize() throws IOException, SecurityException {
        synchronized (this) {
            Persistence.load("./SpertaServer/data/users.txt", userCatalog);
            Persistence.load("./SpertaServer/data/homes.txt", homeCatalog);
        }
    }

    /**
     * Persists the current state of users and houses to files. 
     * Should be called before application shutdown.
     */
    public void persist() throws IOException, SecurityException {
        synchronized (this) {
            Persistence.persist("./SpertaServer/data/users.txt", userCatalog);
            Persistence.persist("./SpertaServer/data/homes.txt", homeCatalog);
        }
    }

    public void printState() {
        System.out.println("Current Repository State:");
        System.out.println("Users:");
        userCatalog.values().forEach(user -> System.out.println("- " + user));
        System.out.println("Houses:");
        homeCatalog.values().forEach(house -> System.out.println("- " + house));
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
                return false;
            }
            homeCatalog.put(house.getId(), house);
        }

        try {
            // If creating the files wasn't possible, the operation fails
            Persistence.createLogFiles(HOUSES_PATH + "/" + house.getName(),
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

    public void postUserCertificate(String userId, byte [] readedCertBytes) throws IOException{
        //data/user_certificates
        File certDir = new File("SpertaServer/data/user_certificates");
        if(!certDir.exists()) certDir.mkdir();
        File certFile = new File(certDir, userId + ".cer");
        FileOutputStream fos = new FileOutputStream(certFile);
        fos.write(readedCertBytes);
        fos.close();
    }

    public byte[] getUserCerByUserId(String userId) {
        try {
            File cerFile = new File(USER_CERTS_PATH + "/" + userId + ".cer");
            return Files.readAllBytes(cerFile.toPath());
        } catch (Exception e) {
            return null;
        }
    }
    
    public byte[] getSectionKeyByUserId(String userId, String homeId, String sectionLabel) throws Exception {
        File keyFile = new File(HOUSES_PATH + homeId + "/" + sectionLabel + "/key." + homeId + "." + sectionLabel + "." + userId);
        if (!keyFile.exists()) throw new Exception("Section Key for user not found");
        return Files.readAllBytes(keyFile.toPath());
    }
}
