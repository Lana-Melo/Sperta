package sperta.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

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
    private static SecretKey key;

    

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
     * @throws IOException might occur when writing and reading from fos or fis
     *                     but File Operations might throw FileNotFoundException
     */
    public static File writeFileHash(String filepath) throws IOException {
        MessageDigest hash = newSha256Digest(); 
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
     * @throws IOException                  - IOException if the file couldn't be read or written
     * @throws SecurityException            - If hash didn't match
     */
    public static void fileNotCorrupted(String filepath) throws IOException, SecurityException{
        File hashFile = new File(filepath + ".hash");
        if (!hashFile.exists()) throw new SecurityException("No hash found for this file");
        if ( hashFile.length() != 32) throw new SecurityException("The correspondent hash of the file has been corrupted");//32 bytes is the SHA-256's output

        MessageDigest md = newSha256Digest();
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
        MessageDigest md = newSha256Digest();
        return md.digest(buf);
    }

    public static SecretKey setPBKey(String password){
        byte[] salt = { (byte) 0x12, (byte) 0x3f, (byte) 0xa1, (byte) 0x33, (byte) 0x81, (byte) 0x3f, (byte) 0xff, (byte) 0x11 };
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 20); // pass, salt, iterations
        SecretKeyFactory kf = newPBEWithHmacSHA256AndAES128Factory();
        key = generatePBESecretKey(kf, keySpec); //Sets SecurityService's key
        return key;
    }

    public static CipherInputStream getCipherInputStream(InputStream is) {
        return new CipherInputStream(is, getCipher(false));
    }

    public static CipherOutputStream getCipherOutputStream(OutputStream os) {
        return new CipherOutputStream(os, getCipher(true));
    }

    private static Cipher getCipher(boolean encryptMode){
        int mode = (encryptMode) ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        Cipher c = getChipherPBEWithHmacSHA256AndAES128Instance();
        try {
            c.init(mode, key);
        } catch (InvalidKeyException e) {
            throw new AssertionError("Invalid key exeption, couldn't initialize cipher");
        }
        return c;
    }

    private static Cipher getChipherPBEWithHmacSHA256AndAES128Instance(){
        try {
            return Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
        } catch ( NoSuchPaddingException| NoSuchAlgorithmException e) {
            throw new AssertionError("Coudln't get a cipher for such algorithm, is there a typo in security service?");
        }
    }

    private static SecretKey generatePBESecretKey(SecretKeyFactory kf, PBEKeySpec keySpec) {
        try {
            return kf.generateSecret(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new AssertionError("Coudln't gernerate the SecretKey");
        }
    }

    private static SecretKeyFactory newPBEWithHmacSHA256AndAES128Factory(){
        try {
            return  SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Missing required algorithm: " + HASH_ALGORITHM, e);
        }
    }

    public static MessageDigest newSha256Digest() { 
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by every Java implementation, this is not supposed to be thrown
            throw new AssertionError("Missing required algorithm: " + HASH_ALGORITHM, e);
        }
    }

}
