package sperta.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;

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
    public static <T extends Serializable> void load(String filename, Map<String, T> dest) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            Map<String, T> loadedMap = (Map<String, T>) in.readObject();
            dest.clear();
            dest.putAll(loadedMap);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
}
