package sperta.domain;

import java.net.Socket;

public class Controller extends Thread {
    
		private Socket socket = null;

        public Controller(Socket inSoc) { // I'm not sure about the level of protection needed for this constructor
			socket = inSoc;
		}
 
		public void run(){
            // TODO: all the interaction with the client (Sofia's task)
        }

}
