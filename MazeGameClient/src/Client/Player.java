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
		Socket sckt = new Socket("localhost",1234);
		DataInputStream dataFromServer = new DataInputStream(new BufferedInputStream(sckt.getInputStream()));
		System.out.println(dataFromServer.readUTF());
	}

}
