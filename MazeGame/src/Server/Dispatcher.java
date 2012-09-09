/**
 * 
 */
package Server;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author harish
 * An instance of this class serves as the thread which receives requests from 
 * Player clients and routes the requests to corresponding server Player threads.
 * TODO: Make this class singleton
 */
public class Dispatcher extends Thread {
	
	public Map<Socket, Player> players;
	
	public Dispatcher(){
		 players = new HashMap<Socket, Player>();
	}
	
	public void run(){
		
	}
}
