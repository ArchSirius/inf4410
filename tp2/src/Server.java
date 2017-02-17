import java.rmi.RemoteException;

public class Server implements API {

	final int CAPACITY;
	final int FALSE_RESULT_RATE;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few arguments");
		}
		int capacity = 0;
		int falseResultRate = 0;
		try {
			capacity = Integer.parseInt(args[0]);
			falseResultRate = Integer.parseInt(args[1]);
		}
		catch (final NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
		}
		final Server server = new Server(capacity, falseResultRate);
		server.run();
	}

	public Server(final int capacity, final int falseResultRate) {
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
	};

	public void run() {
		// TODO
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
	public int getCapacity() {
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
