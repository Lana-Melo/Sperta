package sperta;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.Scanner;
import sperta.utils.MessageCodes;
import sperta.utils.Operation;

public class SpertaClient {

    private static final int DEFAULT_PORT_NUMBER = 22345;

    private static final File CLIENT_JAR = new File("SpertaClient/SpertaClient.jar");

    public static void main(String[] args) {
        /*
         * -> ligar ao server
         * -> enviar tamanho pra atestar
         * -> autenticar user
         * -> loop dos comandos (interacao user)
         * 
         * SpertaClient <serverAddress> <truststore> <password-truststore> <keystore> <password-keystore> <user-id> <password>
         * 
         */
        if (args.length < 7) {
            System.out.println("Usage: SpertaClient <serverAddress> <truststore> <password-truststore> <keystore> <password-keystore> <user-id> <password>");
            System.exit(0);
        }
        
        try {
            String user = args[5];
            String pass = args[6];
            String host = args[0].contains(":") ? args[0].split(":")[0] : args[0];
            int port = args[0].contains(":") ? Integer.parseInt(args[0].split(":")[1]) : DEFAULT_PORT_NUMBER; 
            String keystore = args[3];
            String passwordKeystore = args[4];

            
            System.setProperty("javax.net.ssl.trustStore", args[1]);
            System.setProperty("javax.net.ssl.trustStorePassword", args[2]);


            //SocketFactory sf = SSLSocketFactory.getDefault( );                -> DESCOMENTAR NO FIM, isto é so pra debug e isso
            //SSLSocket clientSocket = (SSLSocket) sf.createSocket(host, port); 

            Socket clientSocket = new Socket(host, port);

            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            
            // atestar
            attestation(out, in, clientSocket);

            System.out.println("ANTES DE CHAMAR FUN AUTHENTICATION");
            // autenticar
            authentication(out, in, user, pass, keystore, passwordKeystore);
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
        //get nonce
        byte[] nonce = new byte[8];
        in.readFully(nonce);
        System.out.println("aqui");
        
        //get fileBytes
        int jarLength = (int) CLIENT_JAR.length();
        byte[] fileBytes = new byte[jarLength + nonce.length];

        try (FileInputStream fis = new FileInputStream(CLIENT_JAR)) {
            int totalRead = 0;
            int bytesRead = 0;
            while (totalRead < jarLength) {
                bytesRead = fis.read(fileBytes, totalRead, jarLength - totalRead);
                if (bytesRead == -1) {throw new IOException("Couldn't read bytes from the JAR");}
                totalRead += bytesRead;
            }
        }
        
        //concatenate && hash
        System.arraycopy(nonce, 0, fileBytes, jarLength, nonce.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(fileBytes);
        out.writeObject(hashed);
        out.flush();

        // receive response from server
        MessageCodes serverResponse = (MessageCodes) in.readObject();
        if (serverResponse == (MessageCodes.OK_ATTEST)) {
            System.out.println(serverResponse);
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
    private static void authentication(ObjectOutputStream out, ObjectInputStream in, String user, String pass, String keystore, String passwordKeystore ) throws Exception {
        Scanner sc = new Scanner(System.in);
        MessageCodes server_response;
        System.out.println("Dentro da FUN AUTHENTICATION antes de do");

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

        if(server_response == (MessageCodes.OK_NEW_USER)){
            System.out.println("Sou um new user e vou mandar meu cert pro server");
            /*FileInputStream kfile = new FileInputStream(keystore);
            KeyStore kstore = KeyStore.getInstance("PKCS12");
            kstore.load(kfile, passwordKeystore.toCharArray()); //password da keystore
            Certificate cert = kstore.getCertificate(user);*/

            //ObjectOutputStream outCer = new ObjectOutputStream(out);
            //outCer.writeObject(cert); //estou a enviar o cert ao server 
            
            //byte [] certBytes = cert.getEncoded();
            //out.writeObject(certBytes);

            File certFile = new File("SpertaClient/security/users/" + user + "/" + user + ".cer");
            sendFile(certFile, out);
            System.out.println("ja mandei meu cert pro server");
            
        }
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
        //TODO It will be needed to create a symetric key for each section and send them to the server 
        //These keys should be encrypted with the user's private key
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

        MessageCodes serverResponse = (MessageCodes) in.readObject();
        System.out.print("Response: " + serverResponse);
        if (serverResponse != MessageCodes.OK){System.out.println(); return;}
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
        System.out.print("Response: " + server_response);
        if (server_response != MessageCodes.OK){System.out.println(); return;}
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
        long size = (long) in.readObject(); 
        System.out.print(", " + size + " (long), seguido de " + size + " bytes de dados.\n");
        byte[] data = new byte[(int) size];
        in.readFully(data);
        File dir = new File("data/clientFiles");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, filename);
        FileOutputStream f_out = new FileOutputStream(f);
        f_out.write(data);
        f_out.close(); 
        System.out.println(new String(data));
    }

        
    private static boolean sendFile(File f, ObjectOutputStream outStream) throws IOException {        
        try (FileInputStream fis = new FileInputStream(f)) {
            outStream.writeLong(f.length());
            byte[] buff = new byte[8192];
            int n;
            while ((n = fis.read(buff)) != -1){
                outStream.write(buff, 0, n);
            }
            
            outStream.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File readFile(ObjectInputStream inputStream, String pathName) throws IOException{
        long fsize = inputStream.readLong();
        File f = new File(pathName);
        try (FileOutputStream fos = new FileOutputStream(f)){
            byte[] buff = new byte[8192];
            long remaining = fsize;

            while (remaining > 0){
                int toRead = (int) Math.min(buff.length, remaining);
                int read = inputStream.read(buff, 0, toRead);
                if (read == -1) throw new EOFException("Stream ended abruptly");
                fos.write(buff, 0, read);
                remaining -= read;
            }

            fos.flush();
        }
        return f;
    }
}