/**
 * 
 */
package Client;

import java.io.IOException;

/**
 * @author harish
 *
 */
public class PlayerMain {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		Player p = new Player();
		p.connectToServer();
		p.go();
	}
}
