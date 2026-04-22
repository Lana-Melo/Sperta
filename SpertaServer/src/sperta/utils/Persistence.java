package sperta.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import sperta.services.LoggerService;
import sperta.services.SecurityService;
import sperta.domain.core.Device;
import sperta.domain.core.House;


public class Persistence {

    private Persistence() {}

    /**
     * Method to load data from a .txt file into a given Map. 
     * The .txt file is expected to contain a serialized Map<String, T>.
     * 
     * @param filename the name of the .txt file to load data from
     * @param dest the Map to load data into
     * @requires the .txt file to exist and contain a serialized Map<String, T>
     * @requires the Map type T to be Serializable 
     * 
     */
    public static <T extends Serializable> void load(String filename, Map<String, T> dest) 
        throws IOException, SecurityException {
        File file = new File(filename);
        boolean fileCreated = file.createNewFile();
        if(!fileCreated) { // file already exists
            readFromTo(file, dest);
        } else {
            SecurityService.writeFileHash(filename);
            writeFilesCipherParams(filename, SecurityService.getEncriptionCipher().getParameters().getEncoded());
        }
    }

    private static <T extends Serializable> void readFromTo(File file, Map<String, T> dest)
        throws IOException, SecurityException {

        byte[] params = readFilesCipherParams(file); // has side effects
        Cipher cipher = SecurityService.getDecryptionCipher(params);

        System.out.println("After removing params this is the file length: " + file.length() + file.getName());

        try (FileInputStream fis = new FileInputStream(file);
            CipherInputStream cis = new CipherInputStream(fis, cipher);){
            byte[] buff = cis.readAllBytes();

            //System.out.println("---------------------------------------------------------");
            //System.out.println("Reading from decrypted content from file " + file.getPath());
            //System.out.println(Arrays.toString(buff));
            //System.out.println("---------------------------------------------------------");
            SecurityService.fileNotCorrupted(file.getPath(), buff);
            
            Map<String, T> loadedMap = desserialize(buff);

            //Map<String, T> loadedMap = (Map<String, T>) buff; //in.readObject();
            dest.clear();
            dest.putAll(loadedMap);
        } catch (ClassNotFoundException e ) {
            throw new SecurityException("Couldn't desserialize content's from file " + file.getName() + ", most likely, contents have been altered");
        }
        
        writeFilesCipherParams(file.getPath(), params); //after reading, we have to write the params again
        //SecurityService.fileNotCorrupted(file.getPath());
        //SecurityService.writeFileHash(file.getPath()); //after a  readFilesChiperParam, we need to update the hash
    }
    
    public static <T extends Serializable> void persist(String filename, Map<String, T> sourceMap)
        throws IOException, SecurityException {

        byte[] params = readFilesCipherParams(new File(filename)); //has side effects on file
        SecurityService.fileNotCorrupted(filename, loadFile(filename, params));
        
        byte[] bytes = serialize(sourceMap);
        SecurityService.writeHashOfFromFile(bytes, filename);

        //System.out.println("---------------------------------------------------------");
        //System.out.println("Reading from decrypted content from file " + filename);
        //System.out.println(Arrays.toString(bytes));
        //System.out.println("---------------------------------------------------------");

        Cipher cipher = SecurityService.getEncriptionCipher();
        try (FileOutputStream fos = new FileOutputStream(filename);
            CipherOutputStream cis = new CipherOutputStream(fos, cipher)){
            cis.write(bytes);
        } 
          
        writeFilesCipherParams(filename, cipher.getParameters().getEncoded()); //writes new cipher params
    }

    public static void createLogFiles(String filepath, String houseName) throws IOException {
        Files.createDirectory(Paths.get(filepath)); //create home dir
        String houseLogFilePath = filepath + "/" + houseName + ".csv";
        Files.createFile((Paths.get(houseLogFilePath)));
        SecurityService.writeFileHash(houseLogFilePath);
        for (SectionType sType : Stream.of(SectionType.values()).toList()){
            if (sType.equals(SectionType.ALL)) continue;
            Files.createDirectory(Paths.get(filepath + "/" + sType.getLabel())); //create dir for every Section Type
        }
    }

    public static void createDeviceLogFile(House home, Device device) throws IOException { 
        String homeFilePath = new LoggerService().determineHousePath(home).toString();
        String deviceLogFile = homeFilePath + "/" + device.getType().getLabel() + "/" + device.getType().getLabel() + device.getId() + ".csv";
        Files.createFile((Paths.get(deviceLogFile)));
        SecurityService.writeFileHash(deviceLogFile);
    }

    // -------------------------------- Helper Functions -------------------------------------

    private static byte[] readFilesCipherParams(File file) throws IOException, SecurityException {
        byte[] params;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")){ 
            raf.seek(file.length() - 4);
            int paramSize = raf.readInt();

            if (paramSize < 0 || paramSize > file.length() - 4){
                throw new SecurityException("File must have been currupted, couldn't read cipher params");
            }
            params = new byte[paramSize];

            long paramLoc = file.length() - 4 - paramSize;
            raf.seek(paramLoc);
            raf.readFully(params);

            //then removes params from the file
            raf.setLength(paramLoc);
        } 
        //SecurityService.writeFileHash(file.toString());
        System.out.println("Read cipher params from file: " + Arrays.toString(params) + " at file: " + file.getPath());
        return params;
    }

    private static void writeFilesCipherParams(String filename, byte[] params) {
        try (FileOutputStream fos = new FileOutputStream(filename, true);
            DataOutputStream dos = new DataOutputStream(fos)
        ){
            System.out.println("Writing cipher params to file: " + Arrays.toString(params) + " at file: " + filename);
            dos.write(params);
            dos.writeInt(params.length); //always 4 bytes
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static <T extends Serializable> Map<String, T> desserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis)){
                return (Map<String, T>) ois.readObject();
        } 
    }

    private static <T extends Serializable> byte[] serialize(Map<String, T> map) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos)){
                oos.writeObject(map);
                return bos.toByteArray();
            }
    }

    private static byte[] loadFile(String filepath, byte[] decryptionParams) throws IOException {
        Cipher cipher = SecurityService.getDecryptionCipher(decryptionParams);
        try (FileInputStream fis = new FileInputStream(filepath);
            CipherInputStream cis = new CipherInputStream(fis, cipher)) {
            return cis.readAllBytes();
        }
    }

}
