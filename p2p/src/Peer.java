import java.io.IOException;
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
		boolean timeflag = true;
		long time = 1;
		ps.initServer();
		while (flag) {
			try {
				ps.setPeer(ps.getSs().accept());
				ps.ss.setSoTimeout(PeerConstants.timedelay);
				if (timeflag) {
					time = System.currentTimeMillis() + PeerConstants.timedelay;
					timeflag = false;
				}
				if (time < System.currentTimeMillis()) {
					flag = false;
					io = new IOOperations(ps.getPeer());
					io.initWrite();
					io.streamWrite("Time over No more player can join.Closing your connection");
					ps.getPeer().close();
				} else {
					gs.putPlayerOnGame(ps.getPeer());
				}

			} 
			catch (SocketTimeoutException e) {
				flag=false;
				System.out.println("Error in accepting the client");
			}
			catch (IOException e) {
				System.out.println("Error in accepting the client");
			}
		}
		
		gs.initiateGame();
		while(true)
		{
			//System.out.println("HI");
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
		while(true){
		System.out.println((String) io.streamRead());
		io.streamWrite("Hi");
		try {
			Thread.sleep(4000);
			System.out.println(ps.peer.getPort());
			System.out.println(ps.getPeer().getPort());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		

	}

	private InetSocketAddress getServerAddress() {
		InetSocketAddress iaddress = new InetSocketAddress(
				PeerConstants.serverAddress, PeerConstants.port);
		return iaddress;
	}
}
