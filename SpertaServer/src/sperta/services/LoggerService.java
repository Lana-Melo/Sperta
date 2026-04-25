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

    private static final String DATA_PATH = "SpertaServer/data/houses";

    /**
     * Writes log to respective files, if a file doesn't exist, creates it
     * Writes to the home's log and if applicable, writes to the device's file
     * Assumes log is a valid entry
     * @param log
     * @throws IOException
     * @throws SecurityException        In case a log file was corrupted
     */
    public void insertLog(LogEntry log) throws IOException, SecurityException {
        //Path houseLogPath = Paths.get(determineHousePath(log).toString() + "/" + log.getHouse().getName() + ".csv");
        Path houseLogPath = determineHousePath(log).resolve(log.getHouse().getName() + ".csv");
        writeLogToFile(houseLogPath, log);
        //if ( log.getOperation().equals(Operation.EC) || log.getOperation().equals(Operation.RD)) {
        if (log.getOperation().equals(Operation.EC)){
            Path devicePath = determineDevicePath(log, DATA_PATH);
            writeCsvLogToFile(devicePath, log);
        }
    }

    /**
     * Gets a List with all last operations of all of the house's devices
     * @param house
     * @return
     * @throws IOException
     */
    public List<String> getLastOperationsOfHouse(House house)
        throws IOException, SecurityException {
        List<SectionType> sections = Stream.of(SectionType.values()).toList(); //get all sections
        return getLastOperationsOfHouseBySection(house, sections);
    }

    public List<String> getLastOperationsOfHouseBySection(
        House house,
        List<SectionType> sections
    ) throws IOException, SecurityException {
        List<String> logs = new ArrayList<>();
        Path housePath = determineHousePath(house);
        //System.out.println(housePath.toString());
        List<String> sCodes = sections
            .stream()
            .map(SectionType::getLabel)
            .toList();
        for (File fileType : housePath.toFile().listFiles()) {
            //System.out.println("DENTRO DO FOR");
            if (!sCodes.contains(fileType.getName())) continue; //if code not in specific sections, skip
            for (File file : fileType.listFiles()) {
                if (file.getName().endsWith(".csv")) { 
                    SecurityService.fileNotCorrupted(file.toString());
                    if (file.length() <= 0) continue;
                    String lastLine = readLastLineOfLog(file.toPath());
                    //System.out.println("Encontrei o conteúdo weeee");
                    logs.add(//this replace firts will remove the extension
                        new StringBuilder().append(file.getName().replaceFirst("\\.csv", ""))
                                           .append(":")
                                           .append((lastLine == null) ? "" : lastLine)
                                           .toString()
                    );
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
    public BufferedReader getAllDeviceHistory(House house, Device device)
        throws SecurityException, IOException{
        Path devicePath = determineDevicePath(
            device,
            determineHousePath(house).toString()
        );
        SecurityService.fileNotCorrupted(devicePath.toString());
        return new BufferedReader(new FileReader(devicePath.toFile()));
    }

    /**
     * Correctly determines House path, assuming log points to a valid house
     * @param log
     * @return
     */
    private Path determineHousePath(LogEntry log) {
        return determineHousePath(log.getHouse());
    }

    public Path determineHousePath(House home) {
        for (File file : new File(DATA_PATH).listFiles()) {
            if (file.getName().equals(home.getName())) {
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
    private Path determineDevicePath(LogEntry log, String housePath) {
        return determineDevicePath(
            log.getDevice(),
            housePath + "/" + log.getHouse().getName()
        );
    }

    /**
     * Correctly determines Device path, assuming housePath points to a valid house's device.
     * Assumes it is being used after determineHousePath, therefore uses it's value
     * @param log
     * @param homePath
     * @return
     */
    private Path determineDevicePath(Device device, String housePath) {
        return Paths.get(
            new StringBuilder(housePath)
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
     * @throws SecurityException  in case log file has been corrupted in some way
     */
    private void writeCsvLogToFile(Path p, LogEntry log) throws IOException, SecurityException {
        if (!Files.exists(p)) Files.createFile(p);
        else SecurityService.fileNotCorrupted(p.toString());
        FileOutputStream fos = new FileOutputStream(p.toFile(), true); //write to file
        fos.write(log.getCsvString().getBytes());
        SecurityService.writeFileHash(p.toString());
        fos.close();
    }

    /**
     * Assumes path is a valid path to a File. If the File doesn't exist, creates it
     * @param p
     * @param log
     * @throws IOException
     * @throws SecurityException         In case the log file has been corrupted in some way
     */
    private void writeLogToFile(Path p, LogEntry log) throws IOException, SecurityException {
        if (!Files.exists(p)) Files.createFile(p);
        else SecurityService.fileNotCorrupted(p.toString());
        FileOutputStream fos = new FileOutputStream(p.toFile()); //write to file
        fos.write(log.toString().getBytes()); //DESCOBRIR METODO Q ESCREVE NO FIM DO FICHEIRO
        SecurityService.writeFileHash(p.toString());
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
    private String readLastLineOfLog(Path pathToLog) throws IOException, SecurityException {
        SecurityService.fileNotCorrupted(pathToLog.toString());
        String lastLine = null;
        try (
            BufferedReader br = new BufferedReader(
                new FileReader(pathToLog.toFile())
            )
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                lastLine = line;
            }
        }
        return lastLine;
    }
}
