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
		try {
			System.out.println("Up = w | Down = s | Left = a | Right = d");
			while (!player.isGameOver) {
				input = (char) br.read();
				if (input == 'w' || input == 'a' || input == 's' || input == 'd') {
					player.userInputs.add((char) input);
					System.out.println("Up = w | Down = s | Left = a | Right = d");
				}
			}

		} catch (IOException e) {

		}
	}
	
}
