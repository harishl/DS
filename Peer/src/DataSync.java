import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DataSync extends Remote {
    boolean backupData(Object obj) throws RemoteException;
}
