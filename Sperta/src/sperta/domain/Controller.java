package sperta.domain;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import sperta.domain.core.House;
import sperta.domain.core.IUser;
import sperta.domain.core.User;
import sperta.services.Repository;
import sperta.utils.MessageCodes;
import sperta.utils.Operation;

public class Controller extends Thread{

        private Socket socket = null;
        private IUser currUser;

        public Controller(Socket inSoc){ // I'm not sure about the level of protection needed for this constructor
                socket = inSoc;
        }
        public void run(){
        // TODO: all the interaction with the client (Sofia's task)
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
                        ope=null; //por agora  ope = in.readObject
                        switch (ope) {
                                case CREATE -> create(outStream, inStream);
                                case ADD    -> add(outStream, inStream);
                                case RD     -> rd(outStream, inStream);
                                case EC     -> ec(outStream, inStream);
                                case RT     -> rt(outStream, inStream);
                                case RH     -> rh(outStream, inStream);
                                //acho q n preciso de default pk o cliente envia sempre à partida um enum 
                        }
                }
        }
        private Object rh(ObjectOutputStream outStream, ObjectInputStream inStream) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String hm = (String) inStream.readObject();
                String d = (String) inStream.readObject();

                
                throw new UnsupportedOperationException("Unimplemented method 'rh'");
        }
        private Object rt(ObjectOutputStream outStream, ObjectInputStream inStream) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String hm = (String) inStream.readObject();
                


                throw new UnsupportedOperationException("Unimplemented method 'rt'");
        }
        private Object ec(ObjectOutputStream outStream, ObjectInputStream inStream) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String hm = (String) inStream.readObject();
                String d = (String) inStream.readObject();
                String value = (String) inStream.readObject();
                /**
                 * atualizar 
                 */
                throw new UnsupportedOperationException("Unimplemented method 'ec'");
        }
        private Object rd(ObjectOutputStream outStream, ObjectInputStream inStream) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String hm = (String) inStream.readObject();
                String s = (String) inStream.readObject();

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                if(repo_hm == null) outStream.writeObject(MessageCodes.NOHM); //home n existe 

                //agora quero ver se o currUser pode fazer rd
                //se ele n é dono n pode  checkUserActionPermissions

                //Fazer novo construtor no log q passo o enum da operation e a string toda
                //depois chamo esse construtor, monta o log e é esse log q eu depois passo pra security service
                

                //else regista dispositivo na casa na secção e outStream.writeObject(MessageCodes.OK);


                

                /**
                 * ler as coisinhas lalal
                 * procurar a casa 
                 * ver se o user é o dono 
                 * adicionar device à secção
                 */
                throw new UnsupportedOperationException("Unimplemented method 'rd'");
        }
        private void add(ObjectOutputStream outStream, ObjectInputStream inStream) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String user = (String) inStream.readObject();
                String hm = (String) inStream.readObject();
                String s = (String) inStream.readObject();

                House repo_hm = Repository.INSTANCE.getHouseById(hm);
                IUser repo_user = Repository.INSTANCE.getUserById(user);
                if(repo_user== null) outStream.writeObject(MessageCodes.NOUSER); //user n existe 
                if( repo_hm == null) outStream.writeObject(MessageCodes.NOHM); //home n existe 
                
                //agora quero ver se o currUser pode fazer add
                //se ele n é dono n pode  checkUserActionPermissions

                //else  adiciono user à secção e outStream.writeObject(MessageCodes.OK); 
                //outStream.flush();
                

                /**
                 * vou procurar pelo user no repo 
                 * se nao existir msg de n existe user NOUSER
                 * 
                 * vou procurar pela casa no repo o id é a string hm??? getHouseById
                 * se nao existir enum de n existe essa casa 
                 * 
                 * depois vejo as permissões pra ver se user pode fazer add
                 * 
                 */
        }
        private void create(ObjectOutputStream outStream, ObjectInputStream inStream) throws ClassNotFoundException, IOException {
                // TODO Auto-generated method stub
                String hm = (String) inStream.readObject();
                
                House house = new House(currUser, hm);
                //nome da casa?
                
                outStream.writeObject(Repository.INSTANCE.addHouse(house) ? MessageCodes.OK : MessageCodes.NOK);
                outStream.flush();
        }
}
