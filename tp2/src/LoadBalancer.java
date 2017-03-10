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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

public class LoadBalancer implements LoadBalancerAPI {

    final static String CONFIG_SERVER_FILE = "../config/servers.properties";
    final static String CONFIG_SHARED_FILE = "../config/shared.properties";

    final ArrayList<ServerAPI> servers;
    int nbTries;
    int index;
    int port;

    public static void main(String[] args) {
        final LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.run();
    }

    public LoadBalancer() {
        servers = new ArrayList<>();
        index = 0;
        initialise();
    }

    private void initServerConfig(final InputStream input) throws IOException {
        final Properties properties = new Properties();
        properties.load(input);
        final String[] hostnames = properties.getProperty("hostnames").split(";");
        for (final String hostname : hostnames) {
            servers.add(loadServerStub(hostname));
        }
    }

    private void initSharedConfig(final InputStream input) throws IOException {
        final Properties properties = new Properties();
        properties.load(input);
        boolean isSecurise = Boolean.parseBoolean(properties.getProperty("securise"));
        nbTries = isSecurise ? 1 : 3;
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
        }
        catch (final IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally
        {
            if (input != null) {
                try {
                    input.close();
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ServerAPI loadServerStub(final String hostname) {
        System.out.println("Connecting to " + hostname);
        ServerAPI stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5001);
            stub = (ServerAPI) registry.lookup("server");
            System.out.println(stub.toString());
            return stub;
        }
        catch (final RemoteException e) {
            System.err.println("Unknown remote exception: " + e.getMessage());
        }
        catch (final NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
        }
        catch (final Exception e) {
          e.printStackTrace();
        }
        return stub;
    }

    public void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            final LoadBalancerAPI stub = (LoadBalancerAPI) UnicastRemoteObject.exportObject(this, 5002);
            final Registry registry = LocateRegistry.getRegistry(5001);     // TODO put this in config file
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
        final List<String> instructions = loadInstructions(path);
        System.out.println(instructions);	// TODO cleanup
        System.out.println(nbTries);
        ArrayList<Integer> results = new ArrayList<>();
        for(String instruction : instructions) {
            ArrayList<Integer> result = tryNServers(instruction);
            try {
                results.add(determineResult(result));
            }
            catch (final Exception e) {
            	// TODO handle error i.e. what to do when a result cannot be determined (too many different results, not enough results, etc.)
                System.err.println("Error: " + e.getMessage());
            }
        }
        return calculateResult(results);
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

    private int calculateResult(final ArrayList<Integer> results) {
       int total = 0;
       for (final Integer result : results) {
           total = (total + result) % 4000;
       }
       return total;
    }

    private ArrayList<Integer> tryNServers(final String instruction) {
        final ArrayList<Integer> results = new ArrayList<Integer>();
        for(int i = 0; i < nbTries; ++i) {
            results.add(sendInstruction(instruction));
        }
        return results;
    }

    private List<String> loadInstructions(final String path) {
        try {
            return Files.readAllLines(Paths.get("../operations/" + path)); // TODO use static constant
        }
        catch (final IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;        // TODO Handle exception
    }

    private int sendInstruction(final String instruction) {
        final ServerAPI server = servers.get(index);
        index = (index + 1) % servers.size();
        final String[] instructions = instruction.split(" ");
        final ServerAPI.Operation operation;
        switch (instructions[0]) {
        case "pell":
        	operation = ServerAPI.Operation.PELL;
        	break;
        case "prime":
        	operation = ServerAPI.Operation.PRIME;
        	break;
        default:
        	operation = null;
        }
        int operand = Integer.parseInt(instructions[1]);
        try {
            return server.doOperation(operation, operand);
        }
        catch (final RemoteException e) {
            e.printStackTrace();
        }
        return 0;       // TODO Handle exception
    }

    private int determineResult(final ArrayList<Integer> values) throws Exception {
        if (nbTries == 1) {
            return values.get(0);
        }
        Collections.sort(values);
        if (values.get(0).equals(values.get(values.size() / 2))) {
            return values.get(0);
        }
        else if (values.get(values.size()).equals(values.get(values.size() / 2))) {
            return values.get(values.size());
        }
        return -1;           // TODO handle exception
    }
}
