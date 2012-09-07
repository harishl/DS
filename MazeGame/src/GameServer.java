import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * 
 */

/**
 * @author harish
 * @author sadesh
 */
public class GameServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		int port = 1234;
		ServerSocket gameServer = new ServerSocket(port);
		while(true){
			System.out.println("Waiting for client");
			Socket client = gameServer.accept();
			System.out.println("Client from " + client.getInetAddress()+ " connected.");
			OutputStream out = client.getOutputStream();
			Date date = new Date();
			byte b[] = date.toString().getBytes();
			out.write(b);
		}
	}

}
