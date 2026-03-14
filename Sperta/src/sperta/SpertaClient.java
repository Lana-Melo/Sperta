package sperta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import sperta.utils.MessageCodes;
import sperta.utils.Operation;

public class SpertaClient {

    private static int DEFAULT_PORT_NUMBER = 22345;
    public static void main(String[] args) {
        /*
         * -> ligar ao server
         * -> enviar tamanho pra atestar
         * -> autenticar user
         * -> loop dos comandos (interacao user)
         * 
         * SpertaClient <serverAddress> <user-id> <password>
         * 
         */
        if (args.length < 3) {
            System.out.println("Usage: SpertaClient <serverAddress> <user-id> <password>");
            System.exit(0);
        }
        
        try {
            String user = args[1];
            String pass = args[2];
            String host = args[0].contains(":") ? args[0].split(":")[0] : args[0];
            int port = args[0].contains(":") ? Integer.parseInt(args[0].split(":")[1]) : DEFAULT_PORT_NUMBER; 
            
            Socket clientSocket = new Socket(host, port);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            // atestar
            attestation(out, in, clientSocket);
            // autenticar
            autentication(out, in, user, pass);
            // loop comandos
            userInteraction(out, in);

            clientSocket.close();

        } catch(ConnectException e){
            System.err.println("Server Not Running");
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends attestation data to the server and validates the response.
     * @param out
     * @param in
     * @param clientSocket
     * @throws Exception
     */
    private static void attestation(ObjectOutputStream out, ObjectInputStream in, Socket clientSocket) throws Exception {
    //jar length ainda tenho de pensar como fazer isto, por enquanto mandamos 0          //ATTENTION PLEASE :)
    //ainda n atesta coisa alguma 
    //como é q o cliente sabe o proprio tamanho ???
        long jarSize = new File(SpertaClient.class.getProtectionDomain().getCodeSource().getLocation().toURI()).length();
        out.writeObject((int) jarSize);
        out.flush();
        MessageCodes server_response = (MessageCodes) in.readObject();
        if (server_response == (MessageCodes.OK)) {
            System.out.println("ATTESTATION OK");
        } else {
            System.out.println("ATTESTATION FAILED");
            clientSocket.close();
            System.exit(0);
        }
    }
    /**
     * Authenticates the user, retrying on wrong password.
     * @param out
     * @param in
     * @param user
     * @param pass
     * @throws Exception
     */
    private static void autentication(ObjectOutputStream out, ObjectInputStream in, String user, String pass) throws Exception {
        Scanner sc = new Scanner(System.in);
        MessageCodes server_response;
        do {
            out.writeObject(user);
            out.writeObject(pass);
            out.flush();
            server_response = (MessageCodes) in.readObject();
            if (server_response == (MessageCodes.WRONG_PWD)) {
                System.out.print("Wrong password, try again.\nPassword: ");
                pass = sc.nextLine();
            }
        } while (server_response == (MessageCodes.WRONG_PWD));

        System.out.println(server_response == (MessageCodes.OK_NEW_USER) ? "New user!" : "Welcome, " + user + "!");
    }
    /**
     * Prints available commands to the user.
     */
    private static void commands() {
        System.out.println("""
            Commands available:
            CREATE <hm>
            ADD <user> <hm> <s>
            RD <hm> <s>
            EC <hm> <d> <int>
            RT <hm>
            RH <hm> <d>
            help
            """);
    }
    /**
     * Main interaction loop, reads and dispatches user commands.
     * @param out
     * @param in
     * @throws Exception
     */
    private static void userInteraction(ObjectOutputStream out, ObjectInputStream in) throws Exception {
        Scanner sc = new Scanner(System.in);
        commands();
        System.out.print("Command:");
        try{
            while(sc.hasNextLine()){
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue; //pra n fazer split de uma line vazia 
                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case "CREATE" -> createHouse(parts,out, in);
                    case "ADD"    -> addUser(parts, out, in);
                    case "RD"     -> registerDevice(parts, out, in);
                    case "EC"     -> sendCommand(parts, out, in);
                    case "RT"     -> receiveLastState(parts, out, in);
                    case "RH"     -> receiveHistory(parts, out, in);
                    case "help"   -> commands();
                    default       -> System.out.println("Invalid command. Type 'help'.");
                }
                System.out.print("Command:");
            }
        } catch (SocketException e) {
            System.err.println("Server disconnected!");
        }
        sc.close();
    }
    /**
     * Sends a CREATE request to create a new house.
     * @param parts
     * @param out
     * @param in
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void createHouse(String [] parts, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        if (!validateNumArgs(parts, 2, "CREATE <hm>")) return;
        //mandar enum operation create 
        out.writeObject(Operation.CREATE);
        out.writeObject(parts[1]); //<hm>
        out.flush();
        System.out.println("Response: " + in.readObject());
    }
    /**
     * Sends an ADD request to add a user to a house section.
     * @param parts
     * @param out
     * @param in
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void addUser(String [] parts, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        if (!validateNumArgs(parts, 4, "ADD <user> <hm> <s>")) return;
        //mandar enum operation add
        out.writeObject(Operation.ADD);
        out.writeObject(parts[1]);  //<user> 
        out.writeObject(parts[2]);  // <hm>
        out.writeObject(parts[3]);  // <s>
        out.flush();
        System.out.println("Response: " + in.readObject());
    }
    /**
     * Sends an RD request to register a device in a house section.
     * @param parts
     * @param out
     * @param in
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void registerDevice(String [] parts, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        if (!validateNumArgs(parts, 3, "RD <hm> <s>")) return;
        //mandar enum operation RD
        out.writeObject(Operation.RD);
        out.writeObject(parts[1]);  // <hm> 
        out.writeObject(parts[2]);  // <s>
        out.flush();
        System.out.println("Response: " + in.readObject());

    }
    /**
     * Sends an EC request to set a device state or timer value.
     * @param parts
     * @param out
     * @param in
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void sendCommand(String [] parts, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        if (!validateNumArgs(parts, 4, "EC <hm> <d> <int>")) return;
        //mandar enum operation EC
        out.writeObject(Operation.EC);
        out.writeObject(parts[1]);  // <hm>
        out.writeObject(parts[2]);  // <d>
        out.writeObject(parts[3]); // <int>
        out.flush();
        System.out.println("Response: " + in.readObject());
    }
    /**
     * Sends an RT request and saves the last state of all devices in a house.
     * @param parts
     * @param out
     * @param in
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void receiveLastState(String [] parts, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        if (!validateNumArgs(parts, 2, "RT <hm>")) return;
        //mandar enum operation RT
        out.writeObject(Operation.RT);
        out.writeObject(parts[1]); // <hm>
        out.flush();

        MessageCodes server_response = (MessageCodes) in.readObject();
        System.out.println("Response: " + server_response);
        if (server_response != MessageCodes.OK) return;
        generateFile(in, "RT_" + parts[1] + ".txt");
    }
    /**
     * Sends an RH request and saves the command history of a device.
     * @param parts
     * @param out
     * @param in
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void receiveHistory(String [] parts, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        if (!validateNumArgs(parts, 3, "RH <hm> <d>")) return;
        //mandar enum operation RH
        out.writeObject(Operation.RH);
        out.writeObject(parts[1]); // <hm>
        out.writeObject(parts[2]); // <d>
        out.flush();

        MessageCodes server_response = (MessageCodes) in.readObject();
        System.out.println("Response: " + server_response);
        if (server_response != MessageCodes.OK) return;
        generateFile(in, "RH_" + parts[1] + "_" + parts[2] + ".csv");
    }

    /**
     * Validates the number of arguments for a command.
     * @param parts
     * @param expected
     * @param usage
     * @return
     */
    private static boolean validateNumArgs(String[] parts, int expected, String usage) {
        if (parts.length == expected) return true;
        System.out.println("Usage: " + usage);
        return false;
    }
    /**
     * Receives data from the server and saves it locally.
     * @param in
     * @param filename
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void generateFile(ObjectInputStream in, String filename) throws IOException, ClassNotFoundException {
        byte[] data = new byte[(int)(long) in.readObject()];
        in.readFully(data);
        File f = new File(filename);
        FileOutputStream f_out = new FileOutputStream(f);
        f_out.write(data);
        f_out.close();
        System.out.println(new String(data)); //mostra conteudo do ficheiro n sei se é preciso sinceramente 
    }

        
}