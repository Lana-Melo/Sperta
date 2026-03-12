package sperta.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

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
    public static <K, T extends Serializable> void load(String filename, Map<K, T> dest) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            Map<K, T> loadedMap = (Map<K, T>) in.readObject();
            dest.clear();
            dest.putAll(loadedMap);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static void createLogFiles(String filepath) throws IOException{
        Files.createDirectory(Paths.get(filepath)); //create home dir
        for (SectionType sType : Stream.of(SectionType.values()).toList()){
            if (sType.equals(SectionType.ALL)) continue;
            Files.createDirectory(Paths.get(filepath + "/" + sType.getLabel())); //create dir for every Section Type
        }
    }

}