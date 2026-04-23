package sperta;

import java.io.ByteArrayInputStream;
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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import sperta.utils.MessageCodes;
import sperta.utils.Operation;
import sperta.utils.SectionType;

public class SpertaClient {

    private static final int DEFAULT_PORT_NUMBER = 22345;

    private static final File CLIENT_JAR = new File("SpertaClient/SpertaClient.jar");
    private static String KEYSTORE_PASS;
    private static String KEYSTORE;
    private static String TRUSTSTORE;
    private static String TRUSTSTORE_PASS;

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

        
            validateFileExists(args[3], "Keystore"); //pra ver se encontrou a keystore

            TRUSTSTORE = args[1];       
            TRUSTSTORE_PASS = args[2];

            KEYSTORE = args[3];
            KEYSTORE_PASS = args[4];
            

            System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE);
            System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASS);
            System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");


            //SocketFactory sf = SSLSocketFactory.getDefault( );                -> DESCOMENTAR NO FIM, isto é so pra debug e isso
            //SSLSocket clientSocket = (SSLSocket) sf.createSocket(host, port); 

            Socket clientSocket = new Socket(host, port);

            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            
            // atestar
            attestation(out, in, clientSocket);

            // autenticar
            authentication(out, in, user, pass);
            // loop comandos
            userInteraction(out, in, user);

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
    private static void authentication(ObjectOutputStream out, ObjectInputStream in, String user, String pass) throws Exception {
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

        if(server_response == (MessageCodes.OK_NEW_USER)){
            System.out.println("Sou um new user e vou mandar meu cert pro server");
            byte[] certBytes = loadKeyStore().getCertificate(user).getEncoded();
            out.writeObject(certBytes);
            out.flush();
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
    private static void userInteraction(ObjectOutputStream out, ObjectInputStream in, String user) throws Exception {
        Scanner sc = new Scanner(System.in);
        commands();
        System.out.print("Command:");
        try{
            while(sc.hasNextLine()){
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue; //pra n fazer split de uma line vazia 
                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case "CREATE" -> createHouse(parts, out, in, user);
                    case "ADD"    -> addUser(parts, out, in, user);
                    case "RD"     -> registerDevice(parts, out, in);
                    case "EC"     -> sendCommand(parts, out, in, user);
                    case "RT"     -> receiveLastState(parts, out, in, user);
                    case "RH"     -> receiveHistory(parts, out, in, user);
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
     * @throws Exception 
     */
    private static void createHouse(String [] parts, ObjectOutputStream out, ObjectInputStream in, String user) throws Exception{
        if (!validateNumArgs(parts, 2, "CREATE <hm>")) return;

        out.writeObject(Operation.CREATE);
        out.writeObject(parts[1]); //<hm>
        out.flush();

        MessageCodes response = (MessageCodes) in.readObject();
        System.out.println("Response: " + response);
        if (response != MessageCodes.OK) return;

        createAndSendSectionKeys(user, out);
    }

    /**
     * Sends an ADD request to add a user to a house section.
     * @param parts
     * @param out
     * @param in
     * @throws Exception 
     */
    private static void addUser(String [] parts, ObjectOutputStream out, ObjectInputStream in, String user) throws Exception{
        if (!validateNumArgs(parts, 4, "ADD <user> <hm> <s>")) return;

        String maintainerId = parts[1];
        String homeId = parts[2];
        String sectionLabel = parts[3];
        boolean needCertificate = getUserCert(maintainerId, user) == null;
        System.out.println("consegui avaliar se preciso do certificado!!" + "preciso? " + needCertificate);

        out.writeObject(Operation.ADD);
        out.writeBoolean(needCertificate);
        out.writeObject(maintainerId);  //<user> 
        out.writeObject(homeId);  // <hm>
        out.writeObject(sectionLabel);  // <s>
        out.flush();

        if(needCertificate){
            System.out.println("IDENTIFICOU QUE PRECISA DO CERTIFICADO");
            receiveAndImportUserCert(in, maintainerId, user); 
        }
        boolean  waitForKey = in.readBoolean();
        if(waitForKey) receiveSKAndSendMantainerSK(in, out, maintainerId, user, homeId, sectionLabel);
        

        System.out.println("Response: " + in.readObject());
    }

    private static void receiveSKAndSendMantainerSK(ObjectInputStream in, ObjectOutputStream out, String maintainerId,
            String user, String homeId, String sectionLabel) throws Exception {
        if(SectionType.ALL.getLabel().equals(sectionLabel)) {
            System.out.println("sectionLabel é all, vou receber minhas chaves de todas as seções");
            for(SectionType st : SectionType.values()) {
                if(st == SectionType.ALL) continue;
                receiveSKAndSendMantainerSK(in, out, maintainerId, user, homeId, st.getLabel());
            }
        } else {
            System.out.println("Lendo a chave da seção " + sectionLabel + " da casa " + homeId);
            byte[] wrappedSectionKey = (byte[]) in.readObject();
            System.out.println("recebi a chave");
            byte[] mantainerSectionKey = generateMantainerSKFromOwnerSK(maintainerId, user, wrappedSectionKey);
            System.out.println("Vou mandar a chave do mantenedor para o server");
            out.writeObject(mantainerSectionKey);
            out.flush();
        }
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
     * @throws Exception 
     */
    private static void sendCommand(String [] parts, ObjectOutputStream out, ObjectInputStream in, String user) throws Exception{
        if (!validateNumArgs(parts, 4, "EC <hm> <d> <int>")) return;
        int deviceValue;
        try {
            deviceValue = Integer.parseInt(parts[3]);
        }catch (NumberFormatException e) {
            System.out.println(MessageCodes.NOK);
            return;
        }
        if (deviceValue < 0 || deviceValue > 600) {
            System.out.println(MessageCodes.NOK);
            return;
        }

        //mandar enum operation EC
        out.writeObject(Operation.EC);
        out.writeObject(parts[1]);  // <hm>
        out.writeObject(parts[2]);  // <d>
        out.flush();

        MessageCodes response = (MessageCodes) in.readObject();
        if(response != MessageCodes.OK){
            System.out.println("Response: " + response);
            return;
        }

        
        byte [] wrappedSectionKey = (byte []) in.readObject();
        SecretKey sectionKey = unwrapKeyWithPrivateKey(wrappedSectionKey, user);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, sectionKey);
        byte [] deviceValueEncrypted = cipher.doFinal(parts[3].getBytes());
        out.writeObject(deviceValueEncrypted);
        out.flush();
        System.out.println("Response: " + in.readObject());
    }

    /**
     * Sends an RT request and saves the last state of all devices in a house.
     * @param parts
     * @param out
     * @param in
     * @throws Exception 
     */
    private static void receiveLastState(String [] parts, ObjectOutputStream out, ObjectInputStream in, String user) throws Exception{
        if (!validateNumArgs(parts, 2, "RT <hm>")) return;
        //mandar enum operation RT
        out.writeObject(Operation.RT);
        out.writeObject(parts[1]); // <hm>
        out.flush();

        MessageCodes serverResponse = (MessageCodes) in.readObject();
        System.out.print("Response: " + serverResponse);
        if (serverResponse != MessageCodes.OK){System.out.println(); return;}

        int nKeys = in.readInt();
        Map<String, SecretKey> sectionKeys = new HashMap<>();
        for (int i = 0; i < nKeys; i++) {
            String section = (String) in.readObject();
            byte [] wrappedSectionKey = (byte []) in.readObject();
            SecretKey sectionKey = unwrapKeyWithPrivateKey(wrappedSectionKey, user);
            sectionKeys.put(section, sectionKey);
        }

        System.out.println("\nA imprimir as keys das sections:");
        for(String section: sectionKeys.keySet()){
            System.out.println("key da section: " + section + ". key = " + sectionKeys.get(section));
        }
        generateFile(in, "RT_" + parts[1] + ".txt", sectionKeys);
    }
    /**
     * 
     * @param in
     * @param filename
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private static void generateFile(ObjectInputStream in, String filename, Map<String, SecretKey> sectionKeys) throws IOException, ClassNotFoundException {
        long size = (long) in.readObject();
        byte[] data = new byte[(int) size];
        in.readFully(data);

        String content = new String(data);
        StringBuilder result = new StringBuilder();

        for (String line : content.split("\n")) {

            try {
                String section = line.split("type=")[1].split(",")[0];
                System.out.println("\nProcessando linha da seção " + section);
                String stateEnc = line.split("state=")[1].split("}")[0];
                System.out.println("estado encriptado: " + stateEnc);

                byte[] encrypted = Base64.getDecoder().decode(stateEnc);

                SecretKey key = sectionKeys.get(SectionType.valueOf(section).getLabel());
                System.out.println("key que vou usar pra desencriptar: " + key);
                byte[] decryptedBytes = decrypt(key, encrypted);

                String state = new String(decryptedBytes, StandardCharsets.UTF_8);
                System.out.println("estado desencriptado: " + state);

                line = line.replace(stateEnc, state);

            } catch (Exception e) {
                System.out.println("Erro a processar linha: " + line);
                System.err.println(e.getMessage());
            }

            result.append(line).append("\n");
        }

        File dir = new File("SpertaClient/data/clientFiles");
        if (!dir.exists()) dir.mkdirs();

        File f = new File(dir, filename);

        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(result.toString().getBytes());
        }

        System.out.println(result);
    }
    

    /**
     * Sends an RH request and saves the command history of a device.
     * @param parts
     * @param out
     * @param in
     * @throws Exception 
     */
    private static void receiveHistory(String [] parts, ObjectOutputStream out, ObjectInputStream in, String user) throws Exception{
        if (!validateNumArgs(parts, 3, "RH <hm> <d>")) return;
        //mandar enum operation RH
        out.writeObject(Operation.RH);
        out.writeObject(parts[1]); // <hm>
        out.writeObject(parts[2]); // <d>
        out.flush();

        MessageCodes server_response = (MessageCodes) in.readObject();
        System.out.print("Response: " + server_response);
        if (server_response != MessageCodes.OK){System.out.println(); return;}
        byte [] wrappedSectionKey = (byte []) in.readObject();
        SecretKey sectionKey = unwrapKeyWithPrivateKey(wrappedSectionKey, user);

        generateFile(in, "RH_" + parts[1] + "_" + parts[2] + ".csv", sectionKey);
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
    private static void generateFile(ObjectInputStream in, String filename,SecretKey sectionKey) throws IOException, ClassNotFoundException {
        long size = (long) in.readObject(); 
        System.out.print(", " + size + " (long), seguido de " + size + " bytes de dados.\n");
        byte[] data = new byte[(int) size];
        in.readFully(data);

        String content = new String(data);
        StringBuilder result = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.contains("state=")) {
            try {
                // extrair parte cifrada
                int start = line.indexOf("state=") + 6;
                int end = line.indexOf("}", start);
                String encryptedBase64 = line.substring(start, end);

                // decode base64 → bytes
                byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

                // chamar tua função decrypt
                byte[] decryptedBytes = decrypt(sectionKey,encryptedBytes);

                //
                String state = new String(decryptedBytes, StandardCharsets.UTF_8);

                line = line.substring(0, start) + state + line.substring(end);

            } catch (Exception e) {
                    System.out.println("Erro ao decifrar linha: " + line);
                }
            }

            result.append(line).append("\n");
        }       
        File dir = new File("SpertaClient/data/clientFiles");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, filename);

        try (FileOutputStream f_out = new FileOutputStream(f)) {
            f_out.write(result.toString().getBytes());
        }
        System.out.println(result.toString());
    }

    /**
     * 
     * @param sectionKey
     * @param encryptedBytes
     * @return
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     * @throws InvalidKeyException 
     * @throws NoSuchPaddingException 
     * @throws NoSuchAlgorithmException 
     */
    private static byte [] decrypt(SecretKey sectionKey, byte [] encryptedBytes) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, sectionKey);
        return cipher.doFinal(encryptedBytes);
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
    
    private static byte[] getUserCert(String maintainerId, String user) {
        /*try (FileInputStream tstoreFile = new FileInputStream(USER_SECURITY_LIB + "/" + user + "/truststore." + user)) {
            KeyStore tstore = KeyStore.getInstance("JCEKS");
            tstore.load(tstoreFile, KEYSTORE_PASS.toCharArray());
            Certificate cert = tstore.getCertificate(maintainerId);
            //System.out.println("cert: " + cert); // null se não encontrou
            return tstore.getCertificate(maintainerId).getEncoded();*/
        try{
            Certificate cert = loadTrustStore().getCertificate(maintainerId);
            return cert.getEncoded();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 
     * @param mantainer
     * @param owner
     * @param ownerWrappedSK
     * @return
     * @throws Exception
     */
    private static byte[] generateMantainerSKFromOwnerSK(String mantainer, String owner, byte[] ownerWrappedSK) throws Exception {
        System.out.println("Gerando a chave do mantenedor a partir da chave do owner");
        byte[] mantainerCerBytes = getUserCert(mantainer, owner);
        SecretKey SectionKey = unwrapKeyWithPrivateKey(ownerWrappedSK, owner);
        return wrapKeyWithPublicKey(SectionKey, mantainerCerBytes);
    }

    /**
     * 
     * @param wrappedKey
     * @param owner
     * @return
     * @throws Exception
     */
    private static SecretKey unwrapKeyWithPrivateKey(byte[] wrappedKey, String owner) throws Exception {
        KeyStore kstore = loadKeyStore();
        PrivateKey privateKey = (PrivateKey) kstore.getKey(owner, KEYSTORE_PASS.toCharArray());
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.UNWRAP_MODE, privateKey);
        return (SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
    }

    /**
     * 
     * @param key
     * @param cerBytes
     * @return
     * @throws Exception
     */
    private static byte[] wrapKeyWithPublicKey(SecretKey key, byte[] cerBytes) throws Exception {
        Certificate cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(cerBytes));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.WRAP_MODE, cert.getPublicKey());
        return cipher.wrap(key);
    }

    /**
     * 
     * @param ownerId
     * @param out
     * @throws Exception
     */
    private static void createAndSendSectionKeys(String ownerId, ObjectOutputStream out) throws Exception {
        for(SectionType section : SectionType.values()){
            if (section == SectionType.ALL) continue;
            byte[] wrappedKey = generateAndWrapKey(ownerId);
            out.writeObject(wrappedKey); //sendkey() metodo auxilar q guilherme estava a fazer mas talvez ja nao faça sentido 
            out.flush();
        }
    }

    /**
     * 
     * @param user
     * @return
     * @throws Exception
     */
    private static byte[] generateAndWrapKey(String user) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        SecretKey sectionKey = kg.generateKey();

        //Certificate cert = CertificateFactory.getInstance("X509")
          //      .generateCertificate(new FileInputStream(USER_SECURITY_LIB + "/" + user + "/" + user + ".cer"));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.WRAP_MODE, loadKeyStore().getCertificate(user).getPublicKey());
        return cipher.wrap(sectionKey);
    }

    /**
     * 
     * @param in
     * @param mantainerId
     * @param user
     * @throws Exception
     */
    private static void receiveAndImportUserCert(ObjectInputStream in, String mantainerId, String user) throws Exception {
        byte[] certBytes = (byte[]) in.readObject();
        Certificate userCert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certBytes));
        
        KeyStore tstore = loadTrustStore();
 
        tstore.setCertificateEntry(mantainerId, userCert);
        File tstoreFile = new File(TRUSTSTORE);
        try (FileOutputStream tstoreOut = new FileOutputStream(tstoreFile)) {
            tstore.store(tstoreOut, TRUSTSTORE_PASS.toCharArray());
            System.out.println("Certificado do mantenedor " + mantainerId + " importado com sucesso para o truststore do usuário " + user);
        }
    }

    /**
     * Validates that a file exists at the given path, exiting the program if not found
     * @param path
     * @param label
     */
    private static void validateFileExists(String path, String file) {
        if (!new File(path).exists()) {
            System.err.println(file + " not found: " + path);
            System.exit(0);
        }
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JCEKS");
        try (FileInputStream fis = new FileInputStream(KEYSTORE)) {
            ks.load(fis, KEYSTORE_PASS.toCharArray());
        }
        return ks;
    }
    /**
     * 
     * @return
     * @throws Exception
     */
    private static KeyStore loadTrustStore() throws Exception {
        KeyStore tstore = KeyStore.getInstance("JCEKS");
        File tstoreFile = new File(TRUSTSTORE);
        if (tstoreFile.exists()) {
            try (FileInputStream tstoreIn = new FileInputStream(tstoreFile)) {
                tstore.load(tstoreIn, TRUSTSTORE_PASS.toCharArray());
            }
        } else {
            tstore.load(null, TRUSTSTORE_PASS.toCharArray()); // cria truststore vazia
        }
        return tstore;
    }
}