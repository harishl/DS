//This Thread process all the system logic
public class ProcessThread extends Thread {
	GameSingleton gs;
	public ProcessThread()
	{
		gs=GameSingleton.getInstance();
	}
public void run()
{
	while(true)
	{
		if(gs.playerRequestQueue.size()!=0)
		{
			Player pl=gs.playerRequestQueue.remove(0);
			//Call game logic for the move
			
			pl.io.streamWrite(pl.ch);
		}
	}
}
}
