package sperta.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import sperta.domain.core.Device;
import sperta.domain.core.House;
import sperta.utils.LogEntry;
import sperta.utils.Operation;
import sperta.utils.SectionType;

/**
 * Class to help with LoggingServices related to SpertaServer
 * 
 * 
 * LoggerService assumes Repository creates all of the valid directories for data storage
 * LoggerService only creates the .csv files for each device
 * 
 */
public class LoggerService {
    private static final String DATA_PATH = "data/houses";

    /**
     * Writes log to respective files, if a file doesn't exist, creates it
     * Writes to the home's log and if applicable, writes to the device's file
     * Assumes log is a valid entry
     * @param log
     * @throws IOException
     */
    public void insertLog(LogEntry log) throws IOException { 
        Path houseLogPath = Paths.get(determineHousePath(log).toString() + "/" + log.getHouse().getName() + ".csv");
        writeLogToFile(houseLogPath , log);
        if (log.getOperation().equals(Operation.EC) || log.getOperation().equals(Operation.RD)){
            System.out.println("Cai no if");
            Path devicePath = determineDevicePath(log, DATA_PATH);
            System.out.println(devicePath);
            writeCsvLogToFile(devicePath, log);
        }
    }

    /**
     * Gets a List with all last operations of all of the house's devices
     * @param house
     * @return
     * @throws IOException
     */
    public List<String> getLastOperationsOfHouse(House house) throws IOException {
        List<SectionType> sections = Stream.of(SectionType.values()).toList(); //get all sections
        return getLastOperationsOfHouseBySection(house, sections);
    }

    public List<String> getLastOperationsOfHouseBySection(House house, List<SectionType> sections) throws IOException {
        List<String> logs = new ArrayList<>();
        Path housePath = determineHousePath(house);
        List<String> sCodes = sections.stream().map(SectionType::getLabel).toList();
        for (File fileType : housePath.toFile().listFiles()){
            if (!sCodes.contains(fileType.getName())) continue; //if code not in specific sections, skip
            logs.add("For section " + fileType.getName());
            for (File file : fileType.listFiles()){
                if (file.getName().endsWith(".csv")){
                    logs.add(file.getName() + ": " + readLastLineOfLog(file.toPath()));
                }
            }
        }
        return logs;
    }

    /**
     * Returns a BufferedReader instead of the contents of the file in case the file gets too big
     * House and Device should be valid instances
     * @param house
     * @param device
     * @return
     */
    public BufferedReader getAllDeviceHistory(House house, Device device) throws FileNotFoundException{
        Path devicePath = determineDevicePath(device, determineHousePath(house).toString());
        return new BufferedReader(new FileReader(devicePath.toFile()));
        //TODO: Esta função está muito simples. Só se pode dar ao utilizador o histórico de dados a que ele tem permissão!
        //Talvez seria boa ideia dividir o getAllDeviceHistory por funções relativas a cada Secção e não a todas as secções
    }

    /**
     * Correctly determines House path, assuming log points to a valid house
     * @param log
     * @return
     */
    private Path determineHousePath(LogEntry log){
        return determineHousePath(log.getHouse());
    }

    public Path determineHousePath(House home) {
        for (File file : new File(DATA_PATH).listFiles()){
            if (file.getName().equals(home.getName())){
                return Paths.get(
                    new StringBuilder(DATA_PATH)
                    .append("/")
                    .append(file.getName())
                    .toString()
                );
            }
        }
        return null; //not found
    }

    /**
     * Correctly determines Device path, assuming log points to a valid house's device.
     * Assumes it is being used after determineHousePath, therefore uses it's value
     * @param log
     * @param homePath
     * @return
     */
    private Path determineDevicePath(LogEntry log, String housePath){
        return determineDevicePath(log.getDevice(), housePath + "/" +log.getHouse().getName());
    }


    /**
     * Correctly determines Device path, assuming housePath points to a valid house's device.
     * Assumes it is being used after determineHousePath, therefore uses it's value
     * @param log
     * @param homePath
     * @return
     */  
    private Path determineDevicePath(Device device, String housePath){
        return Paths.get(new StringBuilder(housePath)
            .append("/")
            .append(device.getType().getLabel())
            .append("/")
            .append(device.getType().getLabel())
            .append(device.getId())
            .append(".csv")
            .toString()
        );
    }

    /**
     * Assumes path is a valid path to a File. If the File doesn't exist, creates it
     * @param p
     * @param log
     * @throws IOException
     */
    private void writeCsvLogToFile(Path p, LogEntry log) throws IOException{
        if (!Files.exists(p)) Files.createFile(p); 
        FileOutputStream fos = new FileOutputStream(p.toFile(), true); //write to file 
        fos.write(log.getCsvString().getBytes());
        fos.close();
    }


    /**
     * Assumes path is a valid path to a File. If the File doesn't exist, creates it
     * @param p
     * @param log
     * @throws IOException
     */
    private void writeLogToFile(Path p, LogEntry log) throws IOException{
        if (!Files.exists(p)) Files.createFile(p); 
        FileOutputStream fos = new FileOutputStream(p.toFile(), true); //write to file 
        fos.write(log.toString().getBytes());  //DESCOBRIR METODO Q ESCREVE NO FIM DO FICHEIRO 
        fos.close();
    }

    /**
     * This algorithm accesses the last line by going through the whole document,
     * there are better ways of doing this, but for sake of simplicity:
     * (pathToLog should be a valid path to a .csv log)
     * @param pathToLog
     * @return
     * @throws IOException
     */
    private String readLastLineOfLog(Path pathToLog) throws IOException {
        String lastLine =  null;
        try (BufferedReader br = new BufferedReader(new FileReader(pathToLog.toFile()))){
            String line; 
            while((line = br.readLine()) != null){
                lastLine = line;
            }
        }
        return lastLine;
    }

}
