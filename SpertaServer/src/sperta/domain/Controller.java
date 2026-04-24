package sperta.domain;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import sperta.domain.core.Device;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.domain.core.User;
import sperta.services.*;
import sperta.utils.*;

/**
 * The Controller class handles the communication with a client over a socket
 * connection. It processes commands sent by the client, interacts with the
 * Repository and Services to perform operations, and sends responses back to
 * the client. Each instance of Controller runs in its own thread, allowing
 * multiple clients to connect simultaneously.
 */
public class Controller extends Thread {

    private static SecureRandom random = new SecureRandom();
    private static final File CLIENT_JAR = new File("SpertaServer/out/SpertaClient.jar");

    private Socket socket = null;
    private IUser currUser;
    private LoggerService logs;

    public Controller(Socket inSoc) {
        socket = inSoc;
        logs = new LoggerService();
    }

    /**
     * The main execution method of the Controller thread. It sets up the input and
     * output streams, performs attestation and authentication, and processes client
     * commands in a loop. The thread can be gracefully shut down using a shutdown
     * hook that closes the socket connection.
     */
    @Override
    public void run() throws SecurityException {
        // Setting handler to gracefully shutdown with Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try (ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream())) {

            // attestation
            if (!attest(outStream, inStream)) {
                socket.close();
                return;
            }

            System.out.println("Vou autenticar user");
            // authentication
            authenticate(outStream, inStream);

            // commands loop
            try {
                operations(outStream, inStream);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // socket.close(); // -> done in finally

        } catch (EOFException e) {
            System.out.println("Client Disconnected, closing socket...");
        } catch (SecurityException e) {
            throw e;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally { // allways close the socket even after an error
            try {
                if (!socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Performs attestation by comparing the size of the JAR file sent by the client
     * with the expected size stored in a text file.
     * 
     * @param outStream
     * @param inStream
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private boolean attest(ObjectOutputStream outStream, ObjectInputStream inStream)
            throws IOException, ClassNotFoundException {

        // get nonce
        byte[] nonce = new byte[8];
        random.nextBytes(nonce);
        outStream.write(nonce);
        outStream.flush();

        // ler o jar a partir do atestation.txt assumindo q se mantei no data

        // provavelvelmente fazer aquilo dos splits e lala
        // ficar com o path + nome do ficheiro

        // get fileBytes
        int clientJarLength = (int) CLIENT_JAR.length();
        byte[] fileBytes = new byte[clientJarLength + nonce.length];

        try (FileInputStream fis = new FileInputStream(CLIENT_JAR)) {
            int totalRead = 0;
            int bytesRead = 0;
            while (totalRead < clientJarLength) {
                bytesRead = fis.read(fileBytes, totalRead, clientJarLength - totalRead);
                if (bytesRead == -1) {
                    throw new EOFException("Couldn't read bytes from the JAR");
                }
                totalRead += bytesRead;
            }
        }

        // concatenate && hash
        System.arraycopy(nonce, 0, fileBytes, clientJarLength, nonce.length);
        MessageDigest md = SecurityService.newSha256Digest();
        byte[] predictedHash = md.digest(fileBytes);

        // Compare
        byte[] incomingHash = (byte[]) inStream.readObject();
        boolean isValid = Arrays.equals(incomingHash, predictedHash);

        outStream.writeObject(isValid ? MessageCodes.OK_ATTEST : MessageCodes.NOK_ATTEST);
        outStream.flush();

        return isValid;
    }

    /**
     * Performs authentication by reading the username and password sent by the
     * client, checking them against the Repository, and sending appropriate
     * responses back
     * to the client.
     * If the user does not exist, a new user is created.
     * 
     * @param outStream
     * @param inStream
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void authenticate(ObjectOutputStream outStream, ObjectInputStream inStream)
            throws ClassNotFoundException, IOException {

        String user;
        String passwd; // ainda quero guardar a pass pra depois gerar o hash
        byte[] salt;
        MessageCodes msg;

        do {
            user = (String) inStream.readObject();
            passwd = (String) inStream.readObject();
            this.currUser = Repository.INSTANCE.getUserById(user);
            msg = currUser == null ? MessageCodes.OK_NEW_USER
                    : Arrays.equals(SecurityService.generateHashedPass(currUser.getSalt(), passwd), currUser.getHash())
                            ? MessageCodes.OK_USER // ao inves de comparar pas vai fazer aquilo de recaulcular hash e
                                                   // ver se é igual ao q esta guardado
                            : MessageCodes.WRONG_PWD;

            if (msg == MessageCodes.OK_NEW_USER) {
                System.out.println("Sou um new user");
                // gerar salt
                salt = SecurityService.generateSalt();
                // gerar hash
                byte[] hashedPass = SecurityService.generateHashedPass(salt, passwd);

                // talvez colocar no security service esa func de gerar salt e gerar hash
                currUser = new User(user, hashedPass, salt); // mudou agora é com o hash e o salt guardar no user o hash
                                                             // e o salt
                Repository.INSTANCE.addUser(currUser);
                System.out.println("WEEEEEEEEE");
                outStream.writeObject(msg);
                outStream.flush();
                System.out.println("ESTA Porrinha");
                // agora a parte do certificado
                // o server espera q o client lhe pass o cer
                // e depois guarda com aquela func postUserCertificate
                // File certFile = readFile(inStream, "SpertaServer/data/user_certificates/" +
                // user + ".cer");

                // System.out.println("Saved at: " + certFile.getAbsolutePath());
                // Repository.INSTANCE.postUserCertificate(user, readedCertBytes);
                byte[] readedCertBytes = (byte[]) inStream.readObject();
                Repository.INSTANCE.postUserCertificate(user, readedCertBytes);
                return;
            }

            if (msg == MessageCodes.WRONG_PWD) {
                System.out.println("USER RECEBIDO: " + user);
                System.out.println("PASS RECEBIDA: " + passwd);
                currUser = null;
            }

            outStream.writeObject(msg);
            outStream.flush();
        } while (currUser == null);
    }

    /**
     * Processes client commands in a loop. Reads the command from the input stream,
     * determines the type of operation, and calls the corresponding method to
     * handle the command.
     * The loop continues until the client disconnects or an error occurs.
     * 
     * @param outStream
     * @param inStream
     * @throws Exception
     */
    private void operations(ObjectOutputStream outStream, ObjectInputStream inStream)
            throws Exception {

        Operation ope;
        while (true) {
            ope = (Operation) inStream.readObject();
            switch (ope) {
                case CREATE -> create(outStream, inStream, ope);
                case ADD -> add(outStream, inStream, ope);
                case RD -> rd(outStream, inStream, ope);
                case EC -> ec(outStream, inStream, ope);
                case RT -> rt(outStream, inStream, ope);
                case RH -> rh(outStream, inStream, ope);
            }
        }
    }

    /**
     * Handles the RH command from the client. Sends a CSV file with the full
     * command history of a device in a house. Only users with permissions over the
     * device's section can receive this information.
     * Command: RH <hm> <d>
     * 
     * Possible responses: - OK: followed by the CSV file with the device history -
     * NOHM: house does not exist - NOD: device does not exist - NOPERM: current
     * user has no permissions over the device's section
     * 
     * @param outStream
     * @param inStream
     * @param ope
     * @throws Exception
     */
    private void rh(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope)
            throws Exception {

        String hm = (String) inStream.readObject();
        String d = (String) inStream.readObject();

        House repoHm = Repository.INSTANCE.getHouseById(hm);
        if (repoHm == null) {
            outStream.writeObject(MessageCodes.NOHM);
            outStream.flush();
            return;
        }

        if (!d.matches("[A-Z]\\d+")) {
            outStream.writeObject(MessageCodes.NOD);
            outStream.flush();
            return;
        }

        String sectionLabel = d.substring(0, 1);
        Long deviceId = Long.valueOf(d.substring(1));

        Device device = repoHm.getDeviceFromSectionById(deviceId, SectionType.ALL.getEnum(sectionLabel));
        if (device == null) {
            outStream.writeObject(MessageCodes.NOD);
            outStream.flush();
            return;
        }

        LogEntry log = new LogEntry(LocalDateTime.now(), currUser, ope, repoHm);
        log.setDevice(device);
        log.setSectionType(SectionType.ALL.getEnum(sectionLabel));
        if (!SecurityService.checkUserActionPermissions(log)) {
            outStream.writeObject(MessageCodes.NOPERM);
            outStream.flush();
            return;
        }

        BufferedReader br = new LoggerService().getAllDeviceHistory(repoHm, device);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        byte[] data = sb.toString().getBytes();
        if (data.length == 0) {
            outStream.writeObject(MessageCodes.NODATA);
            outStream.flush();
            return;
        }
        outStream.writeObject(MessageCodes.OK);
        byte[] sectionKey = Repository.INSTANCE.getSectionKeyByUserId(currUser.getUserId(), hm, sectionLabel);
        outStream.writeObject(sectionKey);
        outStream.writeObject((long) data.length);
        outStream.write(data);
        outStream.flush();
    }

    /**
     * Handles the RT command from the client. Sends a file with the last
     * state/timer value of each device in a house. Only users with permissions over
     * at least one section can receive this information. The information is
     * filtered by the sections the user has access to.
     * 
     * Command: RT <hm>
     * 
     * Possible responses: - OK: followed by the file with the last states - NOHM:
     * house does not exist - NOPERM: current user has no permissions over any
     * section of the house
     * 
     * @param outStream
     * @param inStream
     * @param ope
     * @throws Exception
     */
    private void rt(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope)
            throws Exception {
        String hm = (String) inStream.readObject();

        House repoHm = Repository.INSTANCE.getHouseById(hm);
        if (repoHm == null) {
            outStream.writeObject(MessageCodes.NOHM);
            outStream.flush();
            return;
        }

        LogEntry log = new LogEntry(LocalDateTime.now(), currUser, ope, repoHm);
        if (!(SecurityService.checkUserActionPermissions(log))) {
            outStream.writeObject(MessageCodes.NOPERM);
            outStream.flush();
            return;
        }

        System.out.println("TENHO PERMISSAO na casa MUAHAHAHA");

        List<String> logContent;
        List<SectionType> finalSections = null;
        if (repoHm.getOwner().equals(currUser)) {
            logContent = new LoggerService().getLastOperationsOfHouse(repoHm);
            System.out.println("SOU O OWNER");
            finalSections = Arrays.stream(SectionType.values())
                    .filter(st -> st != SectionType.ALL)
                    .toList();
        } else {

           //Sou mainteiner
            List<SectionType> userSections = SecurityService.getUserAccessibleSectionTypes(repoHm, currUser);

            finalSections =
                    userSections.contains(SectionType.ALL)
                    ? Arrays.stream(SectionType.values())
                            .filter(st -> st != SectionType.ALL) //tenho permissao all
                            .toList()
                    : userSections; //tenho so algumas secçoes

            logContent = new LoggerService().getLastOperationsOfHouseBySection(repoHm, finalSections);
        
        }
        byte[] data = String.join("\n", logContent).getBytes();
        if (data.length == 0) {
            System.out.println("data length é zero que pena");
            outStream.writeObject(MessageCodes.NODATA);
            outStream.flush();
            return;
        }
        outStream.writeObject(MessageCodes.OK);
        System.out.println("mandei o ok e agora vai dar merda");
        outStream.writeInt(finalSections.size());

        for (SectionType st : finalSections) {
            if (st == SectionType.ALL)
                continue;
            byte[] sectionKey = Repository.INSTANCE.getSectionKeyByUserId(currUser.getUserId(), hm, st.getLabel());
            System.out.println("depois do repo");
            outStream.writeObject(st.getLabel());
            System.out.println("mandei a label");
            outStream.writeObject(sectionKey);
        }

        outStream.writeObject((long) data.length);
        outStream.write(data);
        outStream.flush();

    }

    /**
     * Handles the EC command from the client. Sends a state/timer value to a device
     * in a house. Only users with permissions over the device's section can send
     * commands.
     * 
     * Command: EC <hm> <d> <int>
     * 
     * Possible responses: - OK: command sent successfully - NOK: invalid value
     * (must be 0, 1, or ]1..600]) - NOHM: house does not exist - NOD: device does
     * not exist - NOPERM: current user has no permissions over the device's section
     * 
     * @param outStream
     * @param inStream
     * @param ope
     * @throws Exception
     */
    private void ec(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope)
            throws Exception {

        String hm = (String) inStream.readObject();
        String d = (String) inStream.readObject();

        House repoHm = Repository.INSTANCE.getHouseById(hm);
        if (repoHm == null) {
            outStream.writeObject(MessageCodes.NOHM);
            outStream.flush();
            return;
        }

        if (!d.matches("[A-Z]\\d+")) {
            outStream.writeObject(MessageCodes.NOD);
            outStream.flush();
            return;
        }

        String sectionLabel = d.substring(0, 1);
        Long deviceId = Long.valueOf(d.substring(1));
        Device device = repoHm.getDeviceFromSectionById(deviceId, SectionType.ALL.getEnum(sectionLabel));
        if (device == null) {
            outStream.writeObject(MessageCodes.NOD);
            outStream.flush();
            return;
        }

        LogEntry log = new LogEntry(LocalDateTime.now(), currUser, ope, repoHm);
        log.setDevice(device);
        log.setSectionType(SectionType.ALL.getEnum(sectionLabel));
        if (!SecurityService.checkUserActionPermissions(log)) {
            outStream.writeObject(MessageCodes.NOPERM);
            outStream.flush();
            return;
        }

        outStream.writeObject(MessageCodes.OK);

        byte[] sectionKey = Repository.INSTANCE.getSectionKeyByUserId(currUser.getUserId(), hm, sectionLabel);
        outStream.writeObject(sectionKey);

        byte[] encryptedDeviceValue = (byte[]) inStream.readObject();

        log.setDevicesValue(encryptedDeviceValue);
        device.setState(encryptedDeviceValue);
        try {
            logs.insertLog(log);
        } catch (Exception e) {
            outStream.writeObject(MessageCodes.NOK_INTEGRITY);
            outStream.flush();
            throw new SecurityException(e.getMessage());
        }
        outStream.writeObject(MessageCodes.OK);
        outStream.flush();
    }

    /**
     * Handles the RD command from the client. Registers a new device in a section
     * of a house. Only the owner of the house can register devices.
     * 
     * Command: RD <hm> <s>
     * 
     * Possible responses: - OK: device registered successfully - NOHM: house does
     * not exist - NOPERM: current user is not the owner of the house
     * 
     * @param outStream
     * @param inStream
     * @param ope
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void rd(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope)
            throws ClassNotFoundException, IOException {
        String hm = (String) inStream.readObject();
        String s = (String) inStream.readObject();

        if (SectionType.ALL.getEnum(s.toUpperCase()) == null || (!s.equals("all") && s.length() > 1)) {
            outStream.writeObject(MessageCodes.NOK);
            outStream.flush();
            return;
        }

        House repoHm = Repository.INSTANCE.getHouseById(hm);
        if (repoHm == null) {
            outStream.writeObject(MessageCodes.NOHM);
            outStream.flush();
            return;
        }

        LogEntry log = new LogEntry(LocalDateTime.now(), currUser, ope, repoHm);
        if (!(SecurityService.checkUserActionPermissions(log))) {
            outStream.writeObject(MessageCodes.NOPERM);
            outStream.flush();
            return;
        }

        Device repoDevice = repoHm.addDevice(SectionType.ALL.getEnum(s.toUpperCase()));
        log.setDevice(repoDevice);
        try {
            Persistence.createDeviceLogFile(repoHm, repoDevice);
            logs.insertLog(log);
        } catch (Exception e) {
            outStream.writeObject(MessageCodes.NOK_INTEGRITY);
            outStream.flush();
            throw new SecurityException(e.getMessage());
        }
        outStream.writeObject(MessageCodes.OK);
        outStream.flush();
    }

    /**
     * Handles the ADD command from the client. Adds a user as a maintainer of a
     * section in a house. Only the owner of the house can add users.
     * 
     * Command: ADD <user> <hm> <s>
     * 
     * Possible responses: - OK: user added successfully - NOUSER: user does not
     * exist - NOHM: house does not exist - NOPERM: current user is not the owner of
     * the house
     * 
     * @param outStream
     * @param inStream
     * @param ope
     * @throws Exception when the server doesn't find the user certificate
     */
    private void add(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) {
        try {
            boolean needCertificate = inStream.readBoolean();
            String user = (String) inStream.readObject();
            String hm = (String) inStream.readObject();
            String s = (String) inStream.readObject();
            IUser repoUser = Repository.INSTANCE.getUserById(user);
            House repoHm = Repository.INSTANCE.getHouseById(hm);

            if (!addOpParamsAreValid(repoUser, repoHm, s, outStream)) {
                return;
            }
            if (needCertificate) {
                sendUserCertificate(repoUser.getUserId(), outStream);
            }

            SectionType st = SectionType.ALL.getEnum(s);
            if (repoHm.getSection(SectionType.ALL).hasMantainer(repoUser) && !st.equals(SectionType.ALL)) {
                outStream.writeBoolean(false); // inidca ao client q ele nao precisa estar à espera da section key
                for (SectionType section : SectionType.values()) {
                    // para todas as section remover a chave da section do user dessa section
                    // a nao ser q seja a section q queremos adicionar
                    if (section.equals(st) || section.equals(SectionType.ALL))
                        continue;
                    Repository.INSTANCE.removeUserKeySection(hm, section.getLabel(), user);
                }
            } else {
                outStream.writeBoolean(true);
                sendOwnerSK(hm, s, outStream);
                receiveAndImporUserKeyToSection(inStream, hm, s, repoUser.getUserId());
            }

            repoHm.addMantainerToSection(repoUser, SectionType.ALL.getEnum(s));
            outStream.writeObject(MessageCodes.OK);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the CREATE command from the client. Creates a new house with the
     * given name and the current user as owner.
     * 
     * Command: CREATE <hm>
     * 
     * Possible responses: - OK: house created successfully - NOK: house already
     * exists
     * 
     * @param outStream
     * @param inStream
     * @param ope
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void create(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope)
            throws ClassNotFoundException, IOException {
        String hm = (String) inStream.readObject();
        House house = new House(currUser, hm);
        boolean created = Repository.INSTANCE.addHouse(house);
        outStream.writeObject(created ? MessageCodes.OK : MessageCodes.NOK);
        outStream.flush();

        if (!created)
            return;

        for (SectionType section : SectionType.values()) {
            if (section == SectionType.ALL)
                continue;
            // Lana aqui: substituir as linhas debaixo para receiveAndImportSectionKey
            receiveAndImporUserKeyToSection(inStream, hm, section.getLabel(), currUser.getUserId());
        }
    }

    private boolean sendFile(File f, ObjectOutputStream outStream) {
        try (FileInputStream fis = new FileInputStream(f)) {
            outStream.writeLong(f.length());
            byte[] buff = new byte[8192];
            int n;
            while ((n = fis.read(buff)) != -1) {
                outStream.write(buff, 0, n);
            }

            outStream.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private File readFile(ObjectInputStream inputStream, String pathName) throws IOException {
        System.out.println("ANTES DO READLONG");
        long fsize = inputStream.readLong();
        System.out.println("DEPOIS DO READLONG: " + fsize);
        File f = new File(pathName);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            byte[] buff = new byte[8192];
            long remaining = fsize;

            while (remaining > 0) {
                int toRead = (int) Math.min(buff.length, remaining);
                int read = inputStream.read(buff, 0, toRead);
                if (read == -1)
                    throw new EOFException("Stream ended abruptly");
                fos.write(buff, 0, read);
                remaining -= read;
            }

            fos.flush();
        }
        return f;
    }

    /**
     * Gracefully shuts down the Controller thread.
     */
    private void shutdown() {
        System.out.println("\nThread shutting down...");
        try {
            if (!socket.isClosed())
                socket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void receiveAndImporUserKeyToSection(ObjectInputStream inStream, String homeId, String sectionLabel,
            String userId) throws IOException, ClassNotFoundException {
        System.out.println("Recebendo a chave do mantenedor para a seção " + sectionLabel + " da casa " + homeId);
        if (SectionType.ALL.getLabel().equals(sectionLabel)) {
            for (SectionType st : SectionType.values()) {
                if (st == SectionType.ALL)
                    continue;
                receiveAndImporUserKeyToSection(inStream, homeId, st.getLabel(), userId);
            }
        } else {
            byte[] wrappedKey = (byte[]) inStream.readObject();
            Repository.INSTANCE.importUserKeyToSection(wrappedKey, homeId, sectionLabel, userId);
        }
    }

    private void sendOwnerSK(String homeId, String sectionLabel, ObjectOutputStream outStream) throws Exception {
        if (SectionType.ALL.getLabel().equals(sectionLabel)) {
            System.out.println("sectionLabel é all, vou enviar as chaves de todas as seções");
            for (SectionType st : SectionType.values()) {
                if (st == SectionType.ALL)
                    continue;
                System.out.println("Vou enviar a chave da seção " + st.getLabel());
                sendUserSecKey(currUser.getUserId(), homeId, st.getLabel(), outStream);
            }
        } else {
            sendUserSecKey(currUser.getUserId(), homeId, sectionLabel, outStream);
        }
    }

    private void sendUserSecKey(String userId, String homeId, String sectionLabel, ObjectOutputStream outStream)
            throws Exception {
        byte[] keyBytes = Repository.INSTANCE.getSectionKeyByUserId(userId, homeId, sectionLabel);
        outStream.writeObject(keyBytes);
        outStream.flush();
    }

    private void sendUserCertificate(String userId, ObjectOutputStream outStream) throws Exception {
        System.out.println("Vou enviar o cert do user: " + userId);
        byte[] certBytes = Repository.INSTANCE.getUserCerByUserId(userId);
        if (certBytes == null)
            throw new Exception("User cert not found");
        outStream.writeObject(certBytes);
        outStream.flush();
    }

    /**
     * Auxiliar function to ADD operation. It receives the required params for the
     * ADD operation, verify if
     * they are valid and return accordingly. Also, it must receive the OutputStream
     * so it can be capable
     * to send the right message codes to the client, if any param is invalid.
     * 
     * @param repoUser  the retrieved user from repository
     * @param repoHm    the retrieved home from repository
     * @param s         the label of the section
     * @param outStream the OutputStream that represents the communication channel
     *                  with the client application
     * @return true if all params are valid, so the add operation can be executed.
     *         false otherwise.
     */
    private boolean addOpParamsAreValid(IUser repoUser, House repoHm, String s, ObjectOutputStream outStream)
            throws IOException {
        if (repoUser == null) {
            outStream.writeObject(MessageCodes.NOUSER);
            outStream.flush();
            return false;
        }

        if (repoHm == null) {
            outStream.writeObject(MessageCodes.NOHM);
            outStream.flush();
            return false;
        }

        if (SectionType.ALL.getEnum(s) == null) {
            outStream.writeObject(MessageCodes.NOK);
            outStream.flush();
            return false;
        }

        LogEntry log = new LogEntry(LocalDateTime.now(), currUser, Operation.ADD, repoHm);
        if (!(SecurityService.checkUserActionPermissions(log))) {
            outStream.writeObject(MessageCodes.NOPERM);
            outStream.flush();
            return false;
        }

        return true;
    }
}
