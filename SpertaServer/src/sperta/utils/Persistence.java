package sperta.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import sperta.services.LoggerService;
import sperta.services.SecurityService;
import sperta.domain.core.Device;
import sperta.domain.core.House;


public class Persistence {
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
        }
    }

    private static <T extends Serializable> void readFromTo(File file, Map<String, T> dest)
        throws IOException, SecurityException {
        SecurityService.fileNotCorrupted(file.getPath());
        if (file.length() == 0) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, T> loadedMap = (Map<String, T>) in.readObject();
            dest.clear();
            dest.putAll(loadedMap);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static <T extends Serializable> void persist(String filename, Map<String, T> sourceMap)
        throws IOException, SecurityException {
        SecurityService.fileNotCorrupted(filename);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(sourceMap);
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

}
