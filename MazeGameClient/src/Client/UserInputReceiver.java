/**
 * 
 */
package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author harish
 *
 */
public class UserInputReceiver implements Runnable {
	Player player;
	
	public UserInputReceiver(Player p) {
		player = p;
	}
	
	@Override
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		char input;
		while (true) {
			System.out.println("up = w | down = s | left = a | right = d");
			try {
				input = (char) br.read();
				if (input == 'w' || input == 'a' || input == 's' || input == 'd')
					player.userInputs.add(input);
			}
			catch (IOException e) {
				
			}
		}
	}
	
}
