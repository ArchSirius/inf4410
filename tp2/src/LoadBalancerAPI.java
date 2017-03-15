import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the LoadBalancer API.
 *
 */
public interface LoadBalancerAPI extends Remote {

	/**
	 * Execute instructions from specified file.
	 * @param path The name of the file containing the instructions.
	 * @return The sum of all results modulo 4000
	 * @throws RemoteException If an exception occurred.
	 */
	int execute(String path) throws RemoteException;
}
