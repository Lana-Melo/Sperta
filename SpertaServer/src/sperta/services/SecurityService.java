package sperta.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import sperta.domain.core.House;
import sperta.domain.core.ISection;
import sperta.domain.core.IUser;
import sperta.utils.LogEntry;
import sperta.utils.Operation;
import sperta.utils.SectionType;

public enum SecurityService {
    INSTANCE;

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final Random RANDOM = new SecureRandom();

    private static final Set<Operation> OWNER_OPERATIONS = 
        Collections.unmodifiableSet(EnumSet.of(
            Operation.CREATE,
            Operation.ADD,
            Operation.RD
        ));

    public static boolean checkUserActionPermissions(LogEntry log){
        //create, add and register device
        if (log.getHouse().getOwner().equals(log.getUser())) return true; //checks if it's owner
        if (OWNER_OPERATIONS.contains(log.getOperation())) return false; //checks if a non-user is trying to do a owner action

        //enviar valor
        ISection section = log.getHouse().getSection(log.getSectionType());
        //System.out.println(section != null);
        //System.out.println(section.hasMantainer(log.getUser()));
        if (section != null && section.hasMantainer(log.getUser())) return true; //check if it's trying to access a section and if has permissions over it

        if (OWNER_OPERATIONS.contains(log.getOperation())){

        } else if(log.getOperation() == Operation.RH){
            section = log.getHouse().getSection(log.getDevice().getType());
            if (section != null && section.hasMantainer(log.getUser())) return true;
        } else if (log.getOperation() == Operation.RT) {
            for (ISection s : log.getHouse().getSections().values()){
                if (s.hasMantainer(log.getUser())) return true;
            }
        }

        return false;
    }

    /**
     * Returns the types of sections that user has access over in the given house
     * @param h
     * @param u
     * @return
     */
    public static List<SectionType> getUserAccessibleSectionTypes(House h, IUser u){
        return h.getSections().values().stream()  //Should check if user is the house's owner //TODO
                .filter(section -> section.hasMantainer(u))
                .map(s -> s.getType())
                .toList();
    }

    /**
     * Assumes filepath is a valid path to a file
     * Assumes the runtime supports the SHA-256 hash algorithm
     * Creates a .hash of given file (deleting any that previously existed)
     * containing the raw 32-byte SHA-256 digest
     * 
     * @param filepath
     * @return created file
     * @throws NoSuchAlgorithmException
     * @throws IOException might occur when writing and reading from fos or fis
     *                     but File Operations might throw FileNotFoundException
     */
    public static File writeFileHash(String filepath) throws NoSuchAlgorithmException, IOException {
        MessageDigest hash = MessageDigest.getInstance(HASH_ALGORITHM);
        File origin = new File(filepath);

        //Give the MessageDigest the content of the file
        try (FileInputStream fis = new FileInputStream(origin)){
            byte[] buff = new byte[8192];
            int n;
            while ((n = fis.read(buff)) != -1){
                hash.update(buff, 0, n);
            }
        }

        //Hash it and put that content to file.hash
        byte[] hashed = hash.digest();
        File f = new File(filepath + ".hash");
        Files.deleteIfExists(f.toPath());
        Files.createFile(f.toPath());
        try (FileOutputStream fos = new FileOutputStream(f)){
            fos.write(hashed);
        }

        return f;
    }


    /**
     * This function throws if a given file has been corrupted, else does nothing
     * @param filepath                      - path to origin file
     * @throws NoSuchAlgorithmException     - SHA256 algorithm wasn't found
     * @throws IOException                  - IOException if the file couldn't be read or written
     * @throws SecurityException            - If hash didn't match
     */
    public static void fileNotCorrupted(String filepath) throws NoSuchAlgorithmException, IOException, SecurityException{
        File hashFile = new File(filepath + ".hash");
        if (!hashFile.exists()) throw new FileNotFoundException("No hash found for this file");
        if ( hashFile.length() != 32) throw new SecurityException("The correspondent hash of the file has been corrupted");//32 bytes is the SHA-256's output

        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        File file = new File(filepath);

        try (FileInputStream fis = new FileInputStream(file)){
            byte[] buff = new byte[8192];
            int n;
            while ((n = fis.read(buff)) != -1){
                md.update(buff, 0, n);
            }
        }

        byte[] verification = md.digest();
        byte[] actual;
        try (FileInputStream fis = new FileInputStream(hashFile)){
            actual = fis.readAllBytes();
        }
 
        boolean corrupted = !Arrays.equals(actual, verification); 
        if (corrupted) throw new SecurityException("File at: " + filepath + " does not correspond to hash [has probably been corrupted]");

    }   

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] generateHashedPass(byte[] salt, String passwd) {
        byte [] buf = new byte[salt.length+passwd.getBytes().length];
        System.arraycopy(passwd.getBytes(), 0, buf, 0, passwd.getBytes().length);
        System.arraycopy(salt, 0, buf, passwd.getBytes().length, salt.length);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte [] result = md.digest(buf);
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    

}
