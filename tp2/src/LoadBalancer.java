import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

public class LoadBalancer implements LoadBalancerAPI {

    final static String CONFIG_SERVER_FILE = "../config/servers.properties";
    final static String CONFIG_SHARED_FILE = "../config/shared.properties";
    int nbTries;
    ArrayList<ServerAPI> servers;
    int port;

    public static void main(String[] args) {
        final LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.run();
    }

    public LoadBalancer() {
        initialise();
    }

    private void initServerConfig(InputStream input) throws IOException {
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.getInteger(prop.getProperty("port"));
        String[] hostnames = prop.getProperty("hostnames").split(";");
        for (String hostname : hostnames) {
            servers.add(loadServerStub(hostname));
        }
    }

    private void initSharedConfig(InputStream input) throws IOException {
        Properties prop = new Properties();
        prop.load(input);
        boolean securise = Boolean.parseBoolean(prop.getProperty("securise"));
        nbTries = securise ? 1 : 3;
    }

    private void initialise() {
        InputStream input = null;
        try {
            // load server properties file
            input = new FileInputStream(CONFIG_SERVER_FILE);
            initServerConfig(input);

            // load shared properties file
            input = new FileInputStream(CONFIG_SHARED_FILE);
            initSharedConfig(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ServerAPI loadServerStub(String hostname) {
        ServerAPI stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = (ServerAPI) registry.lookup("server");
        } catch (RemoteException e) {
            System.err.println("Unknown remote exception: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
        }
        return stub;
    }

    public void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            final LoadBalancerAPI stub = (LoadBalancerAPI) UnicastRemoteObject.exportObject(this, 0);
            final Registry registry = LocateRegistry.getRegistry();
            registry.rebind("server", stub);
            System.out.println("Load balancer ready.");
        }
        catch (final ConnectException e) {
            System.err.println("Could not connect to RMI registry. Is rmiregistry running?");
            System.err.println();
            System.err.println("Error: " + e.getMessage());
        }
        catch (final Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @Override
    public int execute(String path) throws RemoteException {
        List<String> instructions = loadInstructions(path);
        for(String instruction : instructions) {
            ArrayList<Integer> results = tryNServers(instruction);
            try {
                int result = determineResult(results);
                return result;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        return 0;
        // For each instruction
        // Loop server instances
        // Execute operation
        // If timeout, retry operation
        // Store result
        // Determine result
        // If invalid, retry instruction
        // Update result
        // Send result
    }

    private ArrayList<Integer> tryNServers(String instruction) {
        ArrayList<Integer> results = new ArrayList<Integer>();
        for(int i = 0; i < nbTries; ++i) {
            results.add(sendInstruction(instruction));
        }
        return results;
    }

    private List<String> loadInstructions(final String path) {
        try {
            return Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }

    private int sendInstruction(String instruction) {
        return 1;

    }

    private int determineResult(ArrayList<Integer> values) throws Exception {
        return 0;
    }
}
