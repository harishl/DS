/**
 * 
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author harish
 *
 */
public class UserInputReceiver implements Runnable {
	Peer player;
	
	public UserInputReceiver(Peer peer) {
		player = peer;
	}
	
	@Override
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		char input = 'm';
		try {
			while (!player.canMove) {
				input = (char) br.read();
			}
			
			while (player.canMove) {
				
				if (input == 'w' || input == 'a' || input == 's' || input == 'd' || input == 'x') {
					player.userInputs.add((char) input);
				}
				input = (char) br.read();
			}

		} catch (IOException e) {

		}
	}
	
}
