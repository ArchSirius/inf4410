import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * A thread in which instructions are sent to a server.
 *
 */
public class ServerThread extends Thread {

	// Constants
	final static int SUCCESS_BLOCK_INCREMENT = 5;

	// Member variables
	private final ServerAPI server;
	private final ResultsContainer container;
	private final ServerThreadCallback callback;
	private int successProcessedBlocks = 0;
	private int blockSize              = 1;
	private int head                   = 0;
	private int offset                 = 0;

	/**
	 * Constructor.
	 * @param server The target server.
	 * @param container The ResultsContainer to read instructions and store results.
	 * @param callback (optional) The callback interface.
	 */
	public ServerThread(final ServerAPI server, final ResultsContainer container, final ServerThreadCallback callback) {
		this.server = server;
		this.container = container;
		this.callback = callback;
	}

	/**
	 * Starts a thread computation.
	 */
	public void run() {
		if (!checkNonNull()) {
			return;
		}
		reset();
		try {
			blockSize = server.getCapacity();
		}
		catch (final RemoteException e) {
			onConnectionFailure();
			return;
		}
		// Send all instructions
		while (head < container.size()) {
			// Send task block and save result
			try {
				final ArrayList<Integer> resultBlock = server.doOperations(buildTaskBlock());
				insert(resultBlock);

				// Update head
				head += resultBlock.size();
				++successProcessedBlocks;
				// Increment block size every SUCCESS_BLOCK_INCREMENT successful blocks
				if (successProcessedBlocks % SUCCESS_BLOCK_INCREMENT == 0) {
					++blockSize;
				}
			}
			catch (final RejectedException e) {
				// Decrement block size
				successProcessedBlocks = 0;
				if (blockSize > 1) {
					--blockSize;
				}
			}
			catch (final RemoteException e) {
				e.printStackTrace();
				interrupt();
			}
		}
	}

	/**
	 * Builds a subset of instructions based on server capacity and the scaling factor.
	 * @return An ArrayList of instructions.
	 */
	private ArrayList<String> buildTaskBlock() {
		final ArrayList<String> taskBlock = new ArrayList<>();
		for (offset = 0; offset < blockSize && head + offset < container.size(); ++offset) {
			taskBlock.add(container.get(head + offset).getKey());
		}
		return taskBlock;
	}

	/**
	 * Inserts results in the container.
	 * @param resultBlock The results to insert.
	 */
	private void insert(final ArrayList<Integer> resultBlock) {
		int iRes = 0;
		for (final Integer result : resultBlock) {
			container.get(head + iRes++).getValue().add(result);
		}
	}

	/**
	 * Checks that critical objects are all not null.
	 * @return true if all critical objects are not null.
	 */
	private boolean checkNonNull() {
		return server != null && container != null;
	}

	/**
	 * Resets the member variables to initial state.
	 */
	private void reset() {
		successProcessedBlocks = 0;
		blockSize = 1;
		head = 0;
		offset = 0;
	}

	/**
	 * Calls the callback on failure if set.
	 */
	private void onConnectionFailure() {
		if (callback != null) {
			callback.onFailure(server);
		}
	}
}
