import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
public class BackupRMIClient  {
GameSingleton gs;
    public BackupRMIClient() {
    	gs=GameSingleton.getInstance();
    }

    public  void saveBackupData() {

	String host = null;
	try {
DataObject obj=new DataObject();
obj.setGrid(gs.grid);
obj.setGridSize(gs.gridSize);
obj.setNumTreasures(gs.numTreasures);
obj.setPlayers(gs.playerlist);
	    Registry registry = LocateRegistry.getRegistry(host);
	    DataSync stub = (DataSync) registry.lookup("DataSync");
	    boolean response = stub.backupData(obj);
	    System.out.println("response: " + response);
	} catch (Exception e) {
	    System.err.println("Client exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}