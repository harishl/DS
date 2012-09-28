import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Every Peer will have a ServerSocket and a Socket to maintain a peertopeer
 * network. Each peer has an opportunity to become a Primary server or an
 * backupserver.
 */
public class PeerSocket {
	public ServerSocket getSs() {
		return ss;
	}

	public void setSs(ServerSocket ss) {
		this.ss = ss;
	}

	public Socket getPeer() {
		return peer;
	}

	public void setPeer(Socket peer) {
		this.peer = peer;
	}

	/**
	 * This method tries to initialize a server with a port no 12341.If the port
	 * is in use then it is used by the distributed maze game application only.
	 * so it sleeps for one second tries to create a server socket once again to
	 * check whether that port has been released free due to crash of the main
	 * server.If it is crashed the back up server will create a server socket
	 * using the port no 12341 which is allocated for the primary server of the
	 * game.
	 * 
	 */
	public void initServer() {
		boolean flag = true;
		while (flag) {
			flag = false;
			try {
				ss = new ServerSocket(PeerConstants.port);
			} catch (IOException e) {
				System.out.println("Error Creating in the Serversocket:"
						+ e.getMessage()
						+ "\n Trying to create a Server socket once again");
				flag = true;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					System.out
							.println("Interrupted while making the thread sleep for a while");
				}
			}
		}
	}

	public void bindSocket(InetSocketAddress bindpoint) {
		try {
			if (null != peer) {
				peer.bind(bindpoint);
			} else {
				peer = new Socket(PeerConstants.serverAddress,
						PeerConstants.port);
			}
		} catch (IOException e) {
			System.out
					.println("No hosted game available.Try again later.");
			System.exit(0);
		}
	}

	ServerSocket ss;
	Socket peer;

}
