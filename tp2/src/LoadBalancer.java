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
    int nbTries;
    ArrayList<ServerAPI> servers;
    int index;
    int maxIndex;
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

    private void initServerConfig(InputStream input) throws IOException {
        Properties prop = new Properties();
        prop.load(input);
        String[] hostnames = prop.getProperty("hostnames").split(";");
        maxIndex = hostnames.length;
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
        System.err.println(hostname);
        ServerAPI stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5001);
            stub = (ServerAPI) registry.lookup("server");
            System.out.println(stub.toString());
            return stub;
        } catch (RemoteException e) {
            System.err.println("Unknown remote exception: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
        } catch (Exception e) {
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
        List<String> instructions = loadInstructions(path);
        System.out.println(instructions);
        System.out.println(nbTries);
        ArrayList<Integer> results = new ArrayList<>();
        for(String instruction : instructions) {
            ArrayList<Integer> result = tryNServers(instruction);
            try {
                results.add(determineResult(result));
            } catch (Exception e) {
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

    private int calculateResult(ArrayList<Integer> results) {
       int total = 0;
       for (Integer result : results) {
           total = (total + result) % 4000;
       }
       return total;
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
            return Files.readAllLines(Paths.get("../operations/" + path));
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;        // TODO Handle exception
    }

    private int sendInstruction(String instruction) {
        ServerAPI server = servers.get(index);
        index = (index + 1) % maxIndex;
        String[] instructionArr = instruction.split(" ");
        ServerAPI.Operation operation =
                instructionArr[0].equals("pell") ? ServerAPI.Operation.PELL : ServerAPI.Operation.PRIME;
        int operand = Integer.parseInt(instructionArr[1]);
        try {
            return server.doOperation(operation, operand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;       // TODO Handle exception
    }

    private int determineResult(ArrayList<Integer> values) throws Exception {
        if (nbTries == 1) {
            return values.get(0);
        }
        Collections.sort(values);
        if (values.get(0).equals(values.get(values.size() / 2))) {
            return values.get(0);
        } else if (values.get(values.size()).equals(values.get(values.size() / 2))) {
            return values.get(values.size());
        }
        return -1;           // TODO handle exception
    }
}
