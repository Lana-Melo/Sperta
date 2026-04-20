package sperta.utils;

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
            writeFilesCipherParams(filename, SecurityService.getEncriptionCipher().getParameters().getEncoded());
            SecurityService.writeFileHash(filename);
        }
    }

    private static <T extends Serializable> void readFromTo(File file, Map<String, T> dest)
        throws IOException, SecurityException {

        SecurityService.fileNotCorrupted(file.getPath());
        byte[] params = readFilesCipherParams(file);
        Cipher cipher = SecurityService.getDecriptionCipher(params);

        if (file.length() == 0) return; //nothing to read
        

        try (FileInputStream fis = new FileInputStream(file)){
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            ObjectInputStream in = new ObjectInputStream(cis);

            Map<String, T> loadedMap = (Map<String, T>) in.readObject();
            dest.clear();
            dest.putAll(loadedMap);
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
    
    public static <T extends Serializable> void persist(String filename, Map<String, T> sourceMap)
        throws IOException, SecurityException {

        SecurityService.fileNotCorrupted(filename);
        
        Cipher cipher = SecurityService.getEncriptionCipher();
        try (FileOutputStream fos = new FileOutputStream(filename)){
            CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            ObjectOutputStream out = new ObjectOutputStream(cos);

            out.writeObject(sourceMap);
            out.close();
            writeFilesCipherParams(filename, cipher.getParameters().getEncoded());

            SecurityService.writeFileHash(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }  

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

}
