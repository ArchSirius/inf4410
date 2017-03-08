
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LoadBalancerAPI extends Remote {

   int execute(String path) throws RemoteException;
}
