package sperta.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import sperta.domain.core.House;
import sperta.domain.core.ISection;
import sperta.domain.core.IUser;
import sperta.utils.LogEntry;
import sperta.utils.Operation;
import sperta.utils.SectionType;

public enum SecurityService {
    INSTANCE;

    private static final String HASH_ALGORYTHM = "SHA-256";

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
     * Assumes the OS supports SHA-256 encryption
     * Creates a .hash of given file (deleting any that previously existed)
     * 
     * @param filepath
     * @return created file
     * @throws NoSuchAlgorithmException
     * @throws IOException might occur when writing and reading from fos or fis
     *                     but File Operations might throw FileNotFoundException
     */
    public static File writeFileHash(String filepath) throws NoSuchAlgorithmException, IOException {
        MessageDigest hash = MessageDigest.getInstance(HASH_ALGORYTHM);
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


    public static boolean checkIfFileWasAltered(String filepath) throws NoSuchAlgorithmException, IOException{
        File hashFile = new File(filepath + ".hash");
        if (!hashFile.exists() || hashFile.length() != 32) return false; //32 bytes is the SHA-256's output

        MessageDigest hash = MessageDigest.getInstance(HASH_ALGORYTHM);
        File file = new File(filepath);

        try (FileInputStream fis = new FileInputStream(file)){
            byte[] buff = new byte[8192];
            int n;
            while ((n = fis.read(buff)) != -1){
                hash.update(buff, 0, n);
            }
        }

        byte[] verification = hash.digest();
        byte[] actual;
        try (FileInputStream fis = new FileInputStream(hashFile)){
            actual = fis.readAllBytes();
        }

        return Arrays.equals(actual, verification); // == and .equals() are not good choices, so I opted for this one
    }



}
