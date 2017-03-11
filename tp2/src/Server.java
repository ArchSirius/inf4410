import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class Server implements ServerAPI {

	final int CAPACITY;
	final int FALSE_RESULT_RATE;
	final int PORT_RMI;
	final int PORT_SERVER;
	final static String CONFIG_SERVER_FILE = "../config/server.properties";

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few arguments");
			System.exit(1);
		}
		int capacity = 0;
		int falseResultRate = 0;
		int portRmi = 0;
		int portServer = 0;
		try {
			capacity = Integer.parseInt(args[0]);
			falseResultRate = Integer.parseInt(args[1]);

			FileInputStream input = new FileInputStream(LoadBalancer.CONFIG_SHARED_FILE);
			Properties prop = new Properties();
			prop.load(input);
			boolean securise = Boolean.parseBoolean(prop.getProperty("securise"));
			if (securise) {
				falseResultRate = 0;
			}
			input = new FileInputStream(CONFIG_SERVER_FILE);
      prop = new Properties();
			prop.load(input);
      String parsedPort = prop.getProperty("portRMI");
			portRmi = Integer.parseInt(parsedPort);
      parsedPort = prop.getProperty("portServer");
			portServer = Integer.parseInt(parsedPort);
		}
		catch (final NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		final Server server = new Server(capacity, falseResultRate, portRmi, portServer);
		server.run();
	}

	public Server(final int capacity, final int falseResultRate, final int portRmi, final int portServer) {
		if (capacity < 0) {
			System.err.println("Invalid capacity");
			System.exit(1);
		}
		if (falseResultRate < 0 || falseResultRate > 100) {
			System.err.println("Invalid error rate");
			System.exit(1);
		}
		CAPACITY = capacity;
		FALSE_RESULT_RATE = falseResultRate;
		PORT_RMI = portRmi;
		PORT_SERVER = portServer;
	};

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			final ServerAPI stub = (ServerAPI) UnicastRemoteObject.exportObject(this, PORT_SERVER);
			final Registry registry = LocateRegistry.getRegistry(PORT_RMI);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		}
		catch (final ConnectException e) {
			System.err.println("Could not connect to RMI registry. Is rmiregistry running?");
			System.err.println();
			System.err.println("Error: " + e.getMessage());
		}
		catch (final Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	};

	@Override
	public int pell(final int operand) throws RemoteException {
		if (isError()) {
			return generateRandom4k();
		}
		// Using non-optimal implementation
		return Operations.pell(operand);
	}

	@Override
	public int prime(final int operand) throws RemoteException {
		if (isError()) {
			return generateRandom4k();
		}
		// Using non-optimal implementation
		return Operations.prime(operand);
	}

	@Override
	public int getCapacity() throws RemoteException {
		return CAPACITY;
	}

	private boolean isError() {
		if (FALSE_RESULT_RATE == 0) {
			return false;
		}
		if (FALSE_RESULT_RATE == 100) {
			return true;
		}
		return Math.random() * 100 < FALSE_RESULT_RATE;
	}

	private int generateRandom4k() {
		return (int) (Math.random() * 4000);
	}
}
