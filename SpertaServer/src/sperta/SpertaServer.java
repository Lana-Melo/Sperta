package sperta;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import sperta.domain.Controller;
import sperta.services.Repository;


/**
 * Main server class for the Sperta application.
 * Listens for incoming client connections and handles them using a thread pool.
 * Also manages the backup and persistence of user and house data.
 */
public class SpertaServer {

    private final ExecutorService pool = Executors.newCachedThreadPool(); //java.util.concurrent.Executors.newCachedThreadPool();
    private static final Repository REPOSITORY = Repository.INSTANCE;
    private static final int DEFAULT_PORT_NUMBER = 22345;
    private static String password; //TODO

    //SpertaServer <port> <password-cifra> <keystore> <password-keystore>

    /**
     * Main method to start the Sperta server. 
     * Determines the port number to listen on, loads backup data, and starts the server.
     * @param args
     */
    public static void main(String[] args) {
        //TODO, make sure that the server recognizes the first-boot
        //There is no correct password if there is no data to load
        //If there is data to load, before loading (before instancing server)
        //Verify the data.hash with the given password
        if (args.length < 4) {
            System.out.println("Usage: SpertaServer <port> <password-cifra> <keystore> <password-keystore>");
            System.exit(0);
        }

        System.setProperty("javax.net.ssl.keyStore", args[2]); //<keystore>
        System.setProperty("javax.net.ssl.keyStorePassword", args[3]); //<password-keystore>

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

		/*try{
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );             -> DESCOMENTAR NO FIM, isto é so pra debug e isso
			SSLServerSocket sSoc = (SSLServerSocket) ssf.createServerSocket(port);*/
        try (ServerSocket sSoc = new ServerSocket(port)) {   
            while(true){
                Socket clienSocket = sSoc.accept();
                pool.execute(new Controller(clienSocket));
            }
        } catch (IOException e) {
		    e.printStackTrace();
		} catch (SecurityException e) {
            e.printStackTrace();
            shutdown();
        }
    
    }

    private int getPortNumber(String[] args) {
        int port = DEFAULT_PORT_NUMBER;
        if (args.length == 4) { 
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
