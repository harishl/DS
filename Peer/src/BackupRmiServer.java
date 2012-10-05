import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BackupRmiServer extends Thread implements DataSync {
	GameSingleton gs;
	File file;
	 FileWriter fstream;
	 BufferedWriter out;
	 Registry registry = null;
		DataSync stub = null;
	public BackupRmiServer(String threadid) {
		gs = GameSingleton.getInstance();
		file=new File(gs.filename);
		System.out.println(file.getPath());
		if(file.isFile())
		 file.delete();
		 try {
			file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setName(threadid);
	}

	public boolean backupData(Object obj) {
		boolean updatedflag = true;
		DataObject dataObj = (DataObject) obj;
		gs.gridSize = (int) dataObj.getGridSize();
		gs.grid = (GameEntity[][]) dataObj.getGrid();
		gs.numTreasures = (int) dataObj.getNumTreasures();
		gs.playercounter = (int) dataObj.getPlayercounter();
		System.out.println(gs.numTreasures);
		gs.playerlist = (List<Player>) dataObj.getPlayers();
		gs.crashedPlayersandBackupserver=(List<String>) dataObj.getCrashedPlayersandBackupserver();
		gs.primPlayerIdfromRMI=(String) dataObj.getPrimaryPlayerId();
		System.out
				.println("----" + gs.prepareResponseMsg(gs.getTime()) + "---");
		 try {
			fstream = new FileWriter(file,true);
			 out = new BufferedWriter(fstream);
			   out.append(gs.getTime()+gs.prepareResponseMsg("------"));

			   //Close the output stream
			   out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return updatedflag;
	}

	public void run() {
	
		
		boolean flag = true;
		while (flag) {
			flag = false;
			try {
				// BackupRmiServer obj = new BackupRmiServer();
				stub = (DataSync) UnicastRemoteObject.exportObject(this, 0);
				registry = LocateRegistry.getRegistry();
				registry.bind("DataSync", stub);

				System.err.println("Server ready");
			} catch (ConnectException e) {
				flag = true;
			} catch (Exception e) {

				try {
					registry.unbind("DataSync");
					registry.bind("DataSync", stub);
					System.err.println("Server ready");
				} catch (Exception ee) {
					System.err.println("Server exception: " + ee.toString());
					ee.printStackTrace();
				}
			}
		}
	}
	
	public void stopServer()
	{
		
		try {
			registry.unbind("DataSync");
			registry.bind("DataSync", stub);
			System.err.println("Server ready");
		} catch (Exception ee) {
			System.err.println("Server exception: " + ee.toString());
			ee.printStackTrace();
		}
	}
	

}