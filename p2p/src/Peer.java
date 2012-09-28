import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public class Peer extends Thread {
	PeerSocket ps;
	IOOperations io;
	GameSingleton gs;

	Peer() {
		ps = new PeerSocket();
	}

	Peer(int gridSize, int noOfTressures) {
		ps = new PeerSocket();
		gs = GameSingleton.getInstance();
		gs.setGridSize(gridSize);
		gs.setNumTreasures(noOfTressures);
		gs.populateTreasures();
		try {
			gs.putHostPlayerOnGame();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		boolean flag = true;
		ps.initServer();
		while (flag) {
			try {
				ps.setPeer(ps.getSs().accept());
				ps.ss.setSoTimeout(PeerConstants.timedelay);
				gs.putPlayerOnGame(ps.getPeer());

			} catch (SocketTimeoutException e) {
				flag = false;
				System.out.println("Error in accepting the client");
			} catch (IOException e) {
				System.out.println("Error in accepting the client");
			}
		}

		gs.initiateGame();
		while (true) {
			// System.out.println("HI");
			try {
				Thread.sleep(2000);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void startPlayer() {
		ps.bindSocket(getServerAddress());
		io = new IOOperations(ps.getPeer());
		System.out.println((String) io.streamRead());
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		char input;
		System.out.println((String) io.streamRead());
		while (true) {
			System.out.println("up = w | down = s | left = a | right = d");
			try {
				input = (char) br.read();
				if (input == 'w' || input == 'a' || input == 's'
						|| input == 'd')
					io.streamWrite(input);
			} catch (IOException e) {
				System.out.println("hi");
			}
			System.out.println("From Server"+io.streamRead());
		}

	}

	private InetSocketAddress getServerAddress() {
		InetSocketAddress iaddress = new InetSocketAddress(
				PeerConstants.serverAddress, PeerConstants.port);
		return iaddress;
	}
}
