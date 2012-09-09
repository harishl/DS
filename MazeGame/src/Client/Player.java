package Client;
import java.io.*;
import java.net.*;

/**
 * 
 */

/**
 * @author harish
 *
 */
public class Player {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		Socket server = new Socket("localhost",1234);
		System.out.println("Connected to " + server.getInetAddress());
	}

}
