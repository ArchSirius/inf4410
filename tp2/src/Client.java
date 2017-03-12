/**
 * @author Samuel Lavoie-Marchildon et Samuel Rondeau
 * @created March 12 2017
 * @Description Application client utilisée pour envoyer un tâche au Load balancer.
 */

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

	private final LoadBalancerAPI serverStub;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("You must specify the hostname and the path name");
            System.exit(-1);
        }
        final String distantHostname = args[0];
        final String pathName = args[1];
        final Client client = new Client(distantHostname);
        client.run(pathName);

    }

    public Client(final String hostname) {
        serverStub = loadServerStub(hostname);
    }

    private void run(final String pathName) {
        try {
            int result = serverStub.execute(pathName);
            System.out.println(result);
        } catch (RemoteException e) {
            System.err.println("Error executing function on server: " + e.getMessage());
        }
    }

    private LoadBalancerAPI loadServerStub(final String hostname) {
        LoadBalancerAPI stub = null;
        try {
            final Registry registry = LocateRegistry.getRegistry(hostname, 5001);
            stub = (LoadBalancerAPI) registry.lookup("server");
        }
        catch (final RemoteException e) {
            System.err.println("Unknown remote exception: " + e.getMessage());
        }
        catch (final NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
        }
        return stub;
    }
}
