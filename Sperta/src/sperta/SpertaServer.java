package sperta;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import sperta.domain.Controller;
import sperta.services.Repository;


/**
 * Main server class for the Sperta application.
 * Listens for incoming client connections and handles them using a thread pool.
 * Also manages the backup and persistence of user and house data.
 */
public class SpertaServer {

    private final ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool();
    private static final Repository REPOSITORY = Repository.INSTANCE;
    private static final int DEFAULT_PORT_NUMBER = 22345;

    /**
     * Main method to start the Sperta server. 
     * Determines the port number to listen on, loads backup data, and starts the server.
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("\nStarting Sperta Server...\n");
        SpertaServer server = new SpertaServer();
        int port = server.getPortNumber(args);
        REPOSITORY.printState();
        server.startServer(port);
    }

    /**
     * Method to start the server and listen for incoming connections. 
     * For each new connection, creates a new thread to handle it.
     * 
     * @param port the port number to listen on
     */
    private void startServer(int port) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

		try (ServerSocket sSoc = new ServerSocket(port)) {   
            while(true){
                Socket clienSocket = sSoc.accept();
                pool.execute(new Controller(clienSocket));
            }
        } catch (IOException e) {
		    e.printStackTrace();
		}
    
    }

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

    private void shutdown(){
        System.out.println("\nShutting down the server...");
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
        REPOSITORY.persist();
    }
}
