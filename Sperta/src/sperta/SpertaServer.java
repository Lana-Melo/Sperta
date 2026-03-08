package sperta;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import sperta.domain.Controller;
import sperta.domain.core.IUser;

public class SpertaServer {

    private Map<String, IUser> bkpUserCatalog = new HashMap<>();
    

    private static int DEFAULT_PORT_NUMBER = 22345;
    public static void main(String[] args) {
        SpertaServer server = new SpertaServer();
        int port = getPortNumber(args);
        executeBackup();
		server.startServer(port);
    }

    private void executeBackup(){
        // Standy until we solve the question about how to save and to access data on .txt files
    }

    private void startServer(int port) {
        ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
                Controller newServerThread = new Controller(inSoc);
				newServerThread.start(); // Keep in mind: this call executes the thread.run()
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
    }

    private static int getPortNumber(String [] args) {
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

}
