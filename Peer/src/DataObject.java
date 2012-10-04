import java.io.Serializable;
import java.util.List;


public class DataObject implements Serializable{
	private static final long serialVersionUID = -403250971215465051L;
public GameEntity[][] getGrid() {
	return grid;
}
public void setGrid(GameEntity[][] grid) {
	this.grid = grid;
}
public List<String> crashedPlayersandBackupserver;
public List<String> getCrashedPlayersandBackupserver() {
	return crashedPlayersandBackupserver;
}
public void setCrashedPlayersandBackupserver(
		List<String> crashedPlayersandBackupserver) {
	this.crashedPlayersandBackupserver = crashedPlayersandBackupserver;
}
private GameEntity[][] grid;
int numTreasures;
public int getNumTreasures() {
	return numTreasures;
}
public void setNumTreasures(int numTreasures) {
	this.numTreasures = numTreasures;
}
public int getGridSize() {
	return gridSize;
}
public void setGridSize(int gridSize) {
	this.gridSize = gridSize;
}


int gridSize;
public  List<Player>players;
public List<Player> getPlayers() {
	return players;
}
public void setPlayers(List<Player> players) {
	this.players = players;
}
}
