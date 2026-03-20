package sperta.domain;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import sperta.domain.core.Device;
import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.domain.core.User;
import sperta.services.LoggerService;
import sperta.services.Repository;
import sperta.services.SecurityService;
import sperta.utils.LogEntry;
import sperta.utils.MessageCodes;
import sperta.utils.Operation;
import sperta.utils.Persistence;
import sperta.utils.SectionType;

public class Controller extends Thread{

        private Socket socket = null;
        private IUser currUser;
        private LoggerService logs;

        public Controller(Socket inSoc){ // I'm not sure about the level of protection needed for this constructor
                socket = inSoc;
                logs = new LoggerService();
        }
        public void run(){
        // TODO: all the interaction with the client (Sofia's task)

                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
                //fazer o shutdown gracioso q vai fechar a socket 

                        try {
                                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                                //atestar 
                                if(!attest(outStream, inStream)){ socket.close(); return; }

                                //autenticar 
                                authenticate(outStream, inStream);

                                //loop comandos 
                                operations(outStream, inStream);


                                outStream.close();
                                inStream.close();

                                socket.close();
                                
                        

                        } catch (IOException  | ClassNotFoundException e ) {
                                try {
                                        socket.close();
                                } catch (IOException e1) {
                                        e1.printStackTrace();
                                }
                        }
        
                        

        }
        private void shutdown(){
                System.out.println("\nThread shutting down...");
                try {
                        socket.close();
                } catch (IOException e1) {
                        e1.printStackTrace();
                }
        }
        /**
         * 
         * @param outStream
         * @param inStream
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private boolean attest(ObjectOutputStream outStream, ObjectInputStream inStream ) throws IOException, ClassNotFoundException{
                /**
                 * este n precisa de nada do repository 
                 */
                int jarSize = (int) inStream.readObject(); //o client mandou o tamanho (funcao q ainda tenho de fazer no client)
                File f = new File("data/attestation.txt");

                FileInputStream fin = new FileInputStream(f);
                BufferedInputStream input = new BufferedInputStream(fin);
                byte[] buffer = new byte[1024];
                int bytesRead = input.read(buffer);

                String line = new String(buffer, 0, bytesRead).trim(); //vou buscar o q esta no file txt
                String[] parts = line.split(":");                       
                int jarExpectedSize = Integer.parseInt(parts[1]);  
                input.close();
                
                outStream.writeObject(jarSize == jarExpectedSize ? MessageCodes.OK : MessageCodes.NOK); //mando a msg com ok ou nok
                outStream.flush();
                
                return jarSize == jarExpectedSize;
                
        }

        /**
         * 
         * @param outStream
         * @param inStream
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void authenticate(ObjectOutputStream outStream, ObjectInputStream inStream ) throws ClassNotFoundException, IOException{
                /**
                 * esta func precisa q o repository tenha um metodo getUser(user) e um metodo de newUser(user,passwd)
                 */

                String user = null;
                String passwd = null;
                MessageCodes msg;
                do{
                        user = (String) inStream.readObject();
                        passwd = (String) inStream.readObject();
                        this.currUser = Repository.INSTANCE.getUserById(user); 
                        msg = currUser == null ? MessageCodes.OK_NEW_USER 
                                                : currUser.getPassword().equals(passwd) ? MessageCodes.OK_USER 
                                                : MessageCodes.WRONG_PWD;
                        if(msg == MessageCodes.OK_NEW_USER){ currUser = new User(user, passwd);  Repository.INSTANCE.addUser(new User(user, passwd));} //not sure se é assim
                        if(msg == MessageCodes.WRONG_PWD) currUser = null; //pro loop continuar
                        outStream.writeObject(msg);
                        outStream.flush();
                }while(currUser==null);
        }
        /**
         * 
         * @param outStream
         * @param inStream
         * @throws IOException 
         * @throws ClassNotFoundException 
         */
        private void operations(ObjectOutputStream outStream, ObjectInputStream inStream ) throws ClassNotFoundException, IOException{
                /**
                 *      CREATE <hm>     - repository putHome(home,user)
                 *      ADD <user> <hm> <s> - SecuritySer pra ver as permições e depois repository addUser to Home sei la
                 *      RD <hm> <s>     - Security Ser pra ver se pode e depois repository putDevice() newDevice algo assim
                 *      EC <hm> <d> <int>       - Security ser pra ver se pode e depois repository updateDevice
                 *      RT <hm>                 - Security ser e depois log service -> getLastOperationsOfHouse
                 *      RH <hm> <d>             -Security Service e depois log service -> getAllDeviceHistory
                 *      help
                 * 
                 * 
                 */

                Operation ope;
                while (true) {
                        ope = (Operation) inStream.readObject(); 
                        switch (ope) {
                                case CREATE -> create(outStream, inStream, ope);
                                case ADD    -> add(outStream, inStream, ope);
                                case RD     -> rd(outStream, inStream, ope);
                                case EC     -> ec(outStream, inStream, ope);
                                case RT     -> rt(outStream, inStream, ope);
                                case RH     -> rh(outStream, inStream, ope);
                                //acho q n preciso de default pk o cliente envia sempre à partida um enum 
                        }
                }
        }
        /**
         * Handles the RH command from the client.
         * Sends a CSV file with the full command history of a device in a house.
         * Only users with permissions over the device's section can receive this information.
         * 
         * Command: RH <hm> <d>
         * 
         * Possible responses:
         * - OK: followed by the CSV file with the device history
         * - NOHM: house does not exist
         * - NOD: device does not exist
         * - NOPERM: current user has no permissions over the device's section
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void rh(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String hm = (String) inStream.readObject();
                String d = (String) inStream.readObject();
                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if (repo_hm == null) { outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return; }

                if (!d.matches("[A-Z]\\d+")) { outStream.writeObject(MessageCodes.NOD); outStream.flush(); return; }

                String sectionLabel = d.substring(0, 1);
                Long deviceId = Long.parseLong(d.substring(1));

                Device device = repo_hm.getDeviceFromSectionById(deviceId, SectionType.ALL.getEnum(sectionLabel));
                if (device == null) { outStream.writeObject(MessageCodes.NOD); outStream.flush(); return; }

                LogEntry log = new LogEntry(LocalDateTime.now(), currUser, ope, repo_hm);
                log.setDevice(device);
                log.setSectionType(SectionType.ALL.getEnum(sectionLabel));
                if (!SecurityService.checkUserActionPermissions(log)) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return; }

                BufferedReader br = new LoggerService().getAllDeviceHistory(repo_hm, device);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                byte[] data = sb.toString().getBytes();
                if (data.length == 0) {outStream.writeObject(MessageCodes.NODATA); outStream.flush(); return;}
                outStream.writeObject(MessageCodes.OK);
                outStream.writeObject((long) data.length);
                outStream.write(data);
                outStream.flush();

        }
        /**
         * Handles the RT command from the client.
         * Sends a file with the last state/timer value of each device in a house.
         * Only users with permissions over at least one section can receive this information.
         * The information is filtered by the sections the user has access to.
         * 
         * Command: RT <hm>
         * 
         * Possible responses:
         * - OK: followed by the file with the last states
         * - NOHM: house does not exist
         * - NOPERM: current user has no permissions over any section of the house
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException 
         */
        private void rt(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String hm = (String) inStream.readObject();

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null){ outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return;}

                LogEntry log = new LogEntry(LocalDateTime.now(),currUser,ope,repo_hm);
                if(!(SecurityService.checkUserActionPermissions(log))) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return;}
                
                List<String> logs;
                if (repo_hm.getOwner().equals(currUser)) {
                        logs = new LoggerService().getLastOperationsOfHouse(repo_hm);
                } else {
                        List<SectionType> userSections = new ArrayList<>();
                        for (SectionType st : SectionType.values()) {
                                if (st == SectionType.ALL) continue;
                                if (repo_hm.getSection(st).hasMantainer(currUser)) userSections.add(st);
                        }
                        logs = new LoggerService().getLastOperationsOfHouseBySection(repo_hm, userSections);
                }

                byte[] data = String.join("\n", logs).getBytes();
                if (data.length == 0) {outStream.writeObject(MessageCodes.NODATA); outStream.flush(); return;}
                outStream.writeObject(MessageCodes.OK);
                outStream.writeObject((long) data.length);
                outStream.write(data);
                outStream.flush();

        }
        /**
         * Handles the EC command from the client.
         * Sends a state/timer value to a device in a house.
         * Only users with permissions over the device's section can send commands.
         * 
         * Command: EC <hm> <d> <int>
         * 
         * Possible responses:
         * - OK: command sent successfully
         * - NOK: invalid value (must be 0, 1, or ]1..600])
         * - NOHM: house does not exist
         * - NOD: device does not exist
         * - NOPERM: current user has no permissions over the device's section
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void ec(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                
                String hm = (String) inStream.readObject();
                String d = (String) inStream.readObject();
                String value = (String) inStream.readObject();
                
                int deviceValue;
                try {
                        deviceValue = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                        outStream.writeObject(MessageCodes.NOK); outStream.flush(); return;
                }
                if (deviceValue < 0 || deviceValue > 600) { outStream.writeObject(MessageCodes.NOK); outStream.flush(); return; }

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if (repo_hm == null) { outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return; }

                if (!d.matches("[A-Z]\\d+")) { outStream.writeObject(MessageCodes.NOD); outStream.flush(); return; }

                String sectionLabel = d.substring(0, 1);
                Long deviceId = Long.parseLong(d.substring(1));
                Device device = null;
                for (SectionType st : SectionType.values()) {
                        if (st == SectionType.ALL) continue;
                        device = repo_hm.getDeviceFromSectionById(deviceId, st);
                        if (device != null) break;
                }
                if (device == null) { outStream.writeObject(MessageCodes.NOD); outStream.flush(); return; }

                LogEntry log = new LogEntry(LocalDateTime.now(), currUser, ope, repo_hm);
                log.setDevice(device); //diz ao log o device
                log.setDevicesValue(deviceValue);
                log.setSectionType(SectionType.ALL.getEnum(sectionLabel));
                if (!SecurityService.checkUserActionPermissions(log)) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return; }

                device.setState(deviceValue);
                logs.insertLog(log);
                outStream.writeObject(MessageCodes.OK);
                outStream.flush();
                
        }

        /**
         * Handles the RD command from the client.
         * Registers a new device in a section of a house.
         * Only the owner of the house can register devices.
         * 
         * Command: RD <hm> <s>
         * 
         * Possible responses:
         * - OK: device registered successfully
         * - NOHM: house does not exist
         * - NOPERM: current user is not the owner of the house
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void rd(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String hm = (String) inStream.readObject();
                String s = (String) inStream.readObject();
                
                if (SectionType.ALL.getEnum(s.toUpperCase()) == null|| (!s.equals("all") && s.length() > 1)) { outStream.writeObject(MessageCodes.NOK); outStream.flush(); return;}

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null){ outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return;} 

                LogEntry log = new LogEntry(LocalDateTime.now(),currUser,ope,repo_hm);
                if(!(SecurityService.checkUserActionPermissions(log))) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return;}

                Device repoDevice = repo_hm.addDevice(SectionType.ALL.getEnum(s.toUpperCase()));
                log.setDevice(repoDevice);
                Persistence.createDeviceLogFile(repo_hm, repoDevice);
                logs.insertLog(log);
                outStream.writeObject(MessageCodes.OK);
                outStream.flush();

        }

        /**
         * Handles the ADD command from the client.
         * Adds a user as a maintainer of a section in a house.
         * Only the owner of the house can add users.
         * 
         * Command: ADD <user> <hm> <s>
         * 
         * Possible responses:
         * - OK: user added successfully
         * - NOUSER: user does not exist
         * - NOHM: house does not exist
         * - NOPERM: current user is not the owner of the house
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void add(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String user = (String) inStream.readObject();
                String hm = (String) inStream.readObject();
                String s = (String) inStream.readObject();

                IUser repo_user = Repository.INSTANCE.getUserById(user);
                if (repo_user == null) { outStream.writeObject(MessageCodes.NOUSER); outStream.flush(); return;}

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null) { outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return;}
                
                if (SectionType.ALL.getEnum(s) == null){ outStream.writeObject(MessageCodes.NOK); outStream.flush(); return;}
                
                LogEntry log = new LogEntry(LocalDateTime.now(),currUser,ope,repo_hm);
                if(!(SecurityService.checkUserActionPermissions(log))) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return;}

                repo_hm.addMantainerToSection(repo_user, SectionType.ALL.getEnum(s));
                outStream.writeObject(MessageCodes.OK);
                outStream.flush();
        }
        /**
         * Handles the CREATE command from the client.
         * Creates a new house with the given name and the current user as owner.
         * 
         * Command: CREATE <hm>
         * 
         * Possible responses:
         * - OK: house created successfully
         * - NOK: house already exists
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void create(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String hm = (String) inStream.readObject();
                House house = new House(currUser, hm);
                outStream.writeObject(Repository.INSTANCE.addHouse(house) ? MessageCodes.OK : MessageCodes.NOK);
                outStream.flush();
        }
}
