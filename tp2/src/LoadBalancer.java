/**
 * @author Samuel Lavoie-Marchildon et Samuel Rondeau
 * @created March 12 2017
 * @Description Application LoadBalancer utilisée pour distribuer les charges aux serveurs
 */

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

    final static String CONFIG_LB_FILE = "../config/loadBalancer.properties";
    final static String CONFIG_SHARED_FILE = "../config/shared.properties";
    final static String OPERATIONS_PATH = "../config/operations/";

    final ArrayList<ServerAPI> servers;
    int nbTries;
    int portRmi;
    int portLoadBalancer;
    int serverIndex = 0;

    public static void main(String[] args) {
        final LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.run();
    }

    public LoadBalancer() {
        servers = new ArrayList<>();
        initialise();
    }

    /**
     * Load the servers specified inside the properties file.
     * @param input Input stream used to gather properties
     * @throws IOException
     */
    private void loadServersStub(final InputStream input) throws IOException {
        final Properties properties = new Properties();
        properties.load(input);
        final String[] hostnames = properties.getProperty("hostnames").split(";");
        for (final String hostname : hostnames) {
        	final ServerAPI stub = loadServerStub(hostname);
        	if (stub != null) {
        		servers.add(loadServerStub(hostname));
        	}
        }
    }

    /**
     * Gather the ports specified inside the properties file.
     * @param input Input stream used to gather properties
     * @throws IOException
     */
    private void initPorts(final InputStream input) throws IOException {
        final Properties properties = new Properties();
        properties.load(input);
        String port = properties.getProperty("portRMI");
        portRmi = Integer.parseInt(port);
        port = properties.getProperty("portLoadBalancer");
        portLoadBalancer = Integer.parseInt(port);
    }

    /**
     * Specify the number of instance to send the job specified inside the properties file.
     * @param shared Input stream used to gather the shared properties
     * @param lb Input stream used to gather the load balancer properties
     * @throws IOException
     */
    private void initNbInstance(final InputStream shared, final InputStream lb) throws IOException {
        Properties properties = new Properties();
        properties.load(shared);
        boolean isSecurise = Boolean.parseBoolean(properties.getProperty("securise"));

        properties = new Properties();
        properties.load(lb);
        String insecInst = properties.getProperty("nbInsecureInstance");
        int nbInsecureInstance = Integer.parseInt(insecInst);
        nbTries = isSecurise ? 1 : nbInsecureInstance;
    }

    /**
     * Initialise the load balancer using the params specified in the config files
     */
    private void initialise() {
        InputStream input = null;
        InputStream lbInput = null;
        try {
            // load server properties file
            input = new FileInputStream(CONFIG_LB_FILE);
            initPorts(input);
            input = new FileInputStream(CONFIG_LB_FILE);
            loadServersStub(input);

            // load shared properties file
            input = new FileInputStream(CONFIG_SHARED_FILE);
            lbInput = new FileInputStream(CONFIG_LB_FILE);
            initNbInstance(input, lbInput);
        }
        catch (final IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                    lbInput.close();
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Load a server stub
     * @param hostname The address of the server
     * @return
     */
    private ServerAPI loadServerStub(final String hostname) {
        System.out.println("Connecting to " + hostname);
        ServerAPI stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, portRmi);
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
            final LoadBalancerAPI stub = (LoadBalancerAPI) UnicastRemoteObject.exportObject(this, portLoadBalancer);
            final Registry registry = LocateRegistry.getRegistry(portRmi);
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

    /**
     * Execute the operations specified in the file
     * @param operationsFile Name of the operation file.
     * @return The result from the servers
     * @throws RemoteException
     */
    @Override
    public int execute(String operationsFile) throws RemoteException {
        final List<String> instructions = loadInstructions(operationsFile);
        final ArrayList<Integer> results = new ArrayList<>();
        for (int i = 0; i < instructions.size(); ++i) {
            final String instruction = instructions.get(i);
            final ArrayList<Integer> result = tryNServers(instruction);
            try {
                results.add(determineResult(result));
            }
            catch (final Exception e) {
                --i;
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

    /**
     * Add all the results from the servers in order to produce the return value
     * @param results
     * @return the final result to return to the client
     */
    private int calculateResult(final ArrayList<Integer> results) {
       int total = 0;
       for (final Integer result : results) {
           total = (total + result) % 4000;
       }
       return total;
    }

    /**
     * Try N servers, where N is the amount of server specified when the operations can be insecure.
     * @param instruction Instruction to send to a server
     * @return
     */
    private ArrayList<Integer> tryNServers(final String instruction) {
        final ArrayList<Integer> results = new ArrayList<Integer>();
        for(int i = 0; i < nbTries; ++i) {
            results.add(sendInstruction(instruction));
        }
        return results;
    }

    /**
     * Load the instructions from the instruction file.
     * @param operationsFile Name of the operations file
     * @return Return A list of operation
     */
    private List<String> loadInstructions(final String operationsFile) {
        try {
            return Files.readAllLines(Paths.get(OPERATIONS_PATH + operationsFile));
        }
        catch (final IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;        // TODO Handle exception
    }

    /**
     * Send an instruction to the server
     * @param instruction Representation of the Operation Operand
     * @return The value returned from the server
     */
    private int sendInstruction(final String instruction) {
        final ServerAPI server = servers.get(serverIndex);
        serverIndex = (serverIndex + 1) % servers.size();
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

    /**
     * Determine what is the good result from the servers if they return differents values.
     * @param values A list of results from N servers
     * @return The good result
     * @throws Exception
     */
    private int determineResult(final ArrayList<Integer> values) throws Exception {
        if (nbTries == 1) {
            return values.get(0);
        }
        Collections.sort(values);
        if (values.get(0).equals(values.get(values.size() / 2))) {
            return values.get(0);
        }
        else if (values.get(values.size() - 1).equals(values.get(values.size() / 2))) {
            return values.get(values.size() - 1);
        }
        throw new Exception("Could not determine result with values " + values);
    }
}
