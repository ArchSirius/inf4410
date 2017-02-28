import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("You must specify the hostname and the path name");
            System.exit(-1);
        }
        String distantHostname = args[0];
        String pathName = args[1];
        Client client = new Client(distantHostname);
        client.run(pathName);

    }

    private ServerInterface serverStub = null;

    public Client(String hostname) {
        serverStub = loadServerStub(hostname);
    }

    private ServerInterface loadServerStub(String hostname) {
        ServerInterface stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = (ServerInterface) registry.lookup("server");
        } catch (RemoteException e) {
            System.err.println("Unknown remote exception: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
        }
        return stub;
    }

    private void run(String pathName) {
        try {
            int result = serverStub.execute(pathName);
            System.out.println(result);
        } catch (RemoteException e) {
            System.err.println("Error executing function on server: " + e.getMessage());
        }
    }
}