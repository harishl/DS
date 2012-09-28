/*import java.io.IOException;
import java.net.ServerSocket;
import java.util.TimerTask;

public class AcceptTask extends TimerTask {
	String className="AcceptTask";
	@Override
	public void run() {
		int port = 12341;
	//while (true)
		try {
		PeerSocket ps = new PeerSocket();
		ps.setSs(new ServerSocket(port++));
		ps.setPeer(ps.getSs().accept());
		} catch (IOException e) {
			System.out.println(this.className+"Has error in IO Services");
		}
System.out.println("Game");
	}

}
*/