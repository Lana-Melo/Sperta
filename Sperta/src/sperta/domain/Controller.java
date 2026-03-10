package sperta.domain;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import sperta.domain.core.IUser;
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
                        // currUser = get user(user) do repository 
                        msg = currUser == null ? MessageCodes.OK_NEW_USER 
                                                : currUser.getPassword().equals(passwd) ? MessageCodes.OK_USER 
                                                : MessageCodes.WRONG_PWD;
                        //if(response == MessageCodes.OK_NEW_USER)  new user do repository (user,passwd)
                        if(msg == MessageCodes.WRONG_PWD) currUser = null; //pro loop continuar
                        outStream.writeObject(msg);
                        outStream.flush();
                }while(currUser==null);
        }
        /**
         * 
         * @param outStream
         * @param inStream
         */
        private void operations(ObjectOutputStream outStream, ObjectInputStream inStream ){
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
        }
}
