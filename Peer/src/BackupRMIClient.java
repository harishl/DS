import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
public class BackupRMIClient  {
GameSingleton gs;
    public BackupRMIClient() {
    	gs=GameSingleton.getInstance();
    }

	public void saveBackupData() throws RemoteException, NotBoundException {

		String host = null;

		DataObject obj = new DataObject();
		obj.setGrid(gs.grid);
		obj.setGridSize(gs.gridSize);
		obj.setNumTreasures(gs.numTreasures);
		obj.setPlayers(gs.playerlist);
		obj.setCrashedPlayersandBackupserver(gs.crashedPlayersandBackupserver);
		obj.setPlayercounter(gs.playercounter);
		obj.setPrimaryPlayerId(gs.primaryPlayerId);
		Registry registry = LocateRegistry.getRegistry(host);
		DataSync stub = (DataSync) registry.lookup("DataSync");
		boolean response = stub.backupData(obj);

	}
}