package sperta.domain;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.domain.core.User;
import sperta.services.LoggerService;
import sperta.services.Repository;
import sperta.services.SecurityService;
import sperta.utils.LogEntry;
import sperta.utils.MessageCodes;
import sperta.utils.Operation;
import sperta.utils.SectionType;

public class Controller extends Thread{

        private Socket socket = null;
        private IUser currUser;

        public Controller(Socket inSoc){ // I'm not sure about the level of protection needed for this constructor
                socket = inSoc;
        }
        public void run(){
        // TODO: all the interaction with the client (Sofia's task)

                //Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
                //fazer o shutdown gracioso q vai fechar a socket 

                        try {
                                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                                //atestar 
                                attest(outStream, inStream);

                                //autenticar 
                                authenticate(outStream, inStream);

                                //loop comandos 
                                operations(outStream, inStream);


                                outStream.close();
                                inStream.close();

                                socket.close();

                        } catch (IOException  | ClassNotFoundException e) {
                                e.printStackTrace();
                        }
                
                        

        }
        /**
         * 
         * @param outStream
         * @param inStream
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void attest(ObjectOutputStream outStream, ObjectInputStream inStream ) throws IOException, ClassNotFoundException{
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
                if (jarSize != jarExpectedSize) socket.close();
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
                        if(msg == MessageCodes.OK_NEW_USER)  Repository.INSTANCE.addUser(new User(user, passwd)); //not sure se é assim
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
        private Object rh(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String hm = (String) inStream.readObject();
                String d = (String) inStream.readObject();

                
                throw new UnsupportedOperationException("Unimplemented method 'rh'");
        }
        private void rt(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String hm = (String) inStream.readObject();

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null){ outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return;}

                LogEntry log = new LogEntry(LocalDateTime.now(),currUser,ope,repo_hm);
                if(!(SecurityService.checkUserActionPermissions(log))) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return;}
                
                List<String> logs = new LoggerService().getLastOperationsOfHouse(repo_hm);
                byte[] data = String.join("\n", logs).getBytes();
                outStream.writeObject(MessageCodes.OK);
                outStream.writeObject((long) data.length);
                outStream.write(data);
                outStream.flush();

        }
        private void ec(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                
                String hm = (String) inStream.readObject();
                String d = (String) inStream.readObject();
                String value = (String) inStream.readObject();
                
                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null){ outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return;}

                //procurar device por id 
                //se n encontar NOD

                repo_hm.getDeviceFromSectionById(null, getEnum())

        }

        /**
         * 
         * @param outStream
         * @param inStream
         * @param ope
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private void rd(ObjectOutputStream outStream, ObjectInputStream inStream, Operation ope) throws ClassNotFoundException, IOException {
                String hm = (String) inStream.readObject();
                String s = (String) inStream.readObject();

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null){ outStream.writeObject(MessageCodes.NOHM); outStream.flush(); return;} 

                LogEntry log = new LogEntry(LocalDateTime.now(),currUser,ope,repo_hm);
                if(!(SecurityService.checkUserActionPermissions(log))) { outStream.writeObject(MessageCodes.NOPERM); outStream.flush(); return;}

                repo_hm.addDevice(SectionType.ALL.getEnum(s));
                outStream.writeObject(MessageCodes.OK);
                outStream.flush();

        }

        /**
         * 
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
                
                LogEntry log = new LogEntry(LocalDateTime.now(),currUser,ope,repo_hm);
                if(!(SecurityService.checkUserActionPermissions(log))) { outStream.writeObject(MessageCodes.NOPERM); return;}

                repo_hm.addMantainerToSection(repo_user, SectionType.ALL.getEnum(s));
                outStream.writeObject(MessageCodes.OK);
                outStream.flush();

        }
        /**
         * 
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
