package sperta;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import sperta.domain.Controller;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.domain.core.User;
import sperta.services.Repository;
import sperta.utils.Persistence;
import sperta.utils.SectionType;

public class SpertaServer {

    private final ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool();
    private final Map<String, IUser> bkpUsersCatalog = new HashMap<>();
    private final Map<String, House> bkpHomesCatalog = new HashMap<>();
    private static boolean running;
    private static final Repository repository = Repository.INSTANCE;
    private static final int DEFAULT_PORT_NUMBER = 22345;


    public static void main(String[] args) {
        System.out.println("Starting Sperta Server...\n");
        SpertaServer server = new SpertaServer();
        int port = server.getPortNumber(args);
        
        // remover depois
        //cadastraUsersMock();
        //cadastraHomesMock();
        // remover depois

        server.executeBackup();
		server.startServer(port);
    }

    /** 
     * Method to load users and homes from .txt files into backups catalogs, to be used in case of server failure
     */
    private void executeBackup(){
        Persistence.load("./data/users.txt", this.bkpUsersCatalog);
        // FOR TESTING
        System.out.println("\nRetrieved users from .txt file:");
        for (IUser user : bkpUsersCatalog.values()) {
            System.out.println(user); 
        }
        ////////////////
        
        Persistence.load("./data/homes.txt", this.bkpHomesCatalog);
        
        // FOR TESTING
        System.out.println("\nRetrieved homes from .txt file:");
        for (House home : bkpHomesCatalog.values()) {
            System.out.println("\n" + home);
        }
        ////////////////
    }
    
    /**
     * Method to start the server and listen for incoming connections. For each new connection, creates a new thread to handle it.
     * 
     * @param port the port number to listen on
     */
    private void startServer(int port) {
        running = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

		try (ServerSocket sSoc = new ServerSocket(port)) {   
            while(running){
                try (Socket clienSocket = sSoc.accept()) {
                    pool.execute(new Controller(clienSocket));
		        } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
		    e.printStackTrace();
		}
    
    }

    private void shutdown(){
        System.out.println("\nShutting down the server...");
        running = false;
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Persisting data...");
        repository.persist();
    }
    /**
     * Method to determine the port number to be used by the server. 
     * If no port number is given as an argument, or the given port number is invalid, uses the default port number.
     * 
     * @param args the command line arguments
     * @return the port number to be used by the server
     */
    private int getPortNumber(String[] args) {
        int port = DEFAULT_PORT_NUMBER;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("The given port number is invalid. Initializing the program with the default port number...\n");
                port = DEFAULT_PORT_NUMBER;
            }
        }
        return port;
    }

    // APAGAR DEPOIS ESTE DOIS MÉTODOS DE MOCK, SÃO APENAS PARA TESTE
    private static void cadastraHomesMock() {
        System.out.println("creating mock homes...");

        IUser user1 = new User("Guilherme", "1234");
        IUser user2 = new User("Sofia", "4321");
        IUser user3 = new User("Lana", "5678");

        House home1 = new House(user1);
        home1.setName("Casa do Guilherme");
        House home2 = new House(user2);
        home2.setName("Casa da Sofia");
        House home3 = new House(user3);
        home3.setName("Casa da Lana");

        home1.addDevice(SectionType.ELECTROS);
        home2.addDevice(SectionType.GARDEN);
        home3.addDevice(SectionType.LUZES);

        Map<String, House> mockHomesMap = new HashMap<>();

        mockHomesMap.put(home1.getName(), home1);
        mockHomesMap.put(home2.getName(), home2);
        mockHomesMap.put(home3.getName(), home3);

        try (ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream("./data/homes.txt"))) {

            out.writeObject(mockHomesMap);

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("mock homes saved!");

    }    
    private static void cadastraUsersMock() {
        System.out.println("creating mock users...");

        IUser user1 = new User("Guilherme", "1234");
        IUser user2 = new User("Sofia", "4321");
        IUser user3 = new User("Lana", "5678");

        Map<String, IUser> mockUsersMap = new HashMap<>();

        mockUsersMap.put(user1.getUserId(), user1);
        mockUsersMap.put(user2.getUserId(), user2);
        mockUsersMap.put(user3.getUserId(), user3);

        try (ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream("./data/users.txt"))) {

            out.writeObject(mockUsersMap);

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("mock users saved!");
    }

}
