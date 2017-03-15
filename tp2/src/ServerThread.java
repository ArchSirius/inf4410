import java.rmi.RemoteException;
import java.util.ArrayList;

public class ServerThread extends Thread {

	final static int SUCCESS_BLOCK_INCREMENT = 5;

	private final ServerAPI server;
	private final ResultsContainer container;
	private final ServerThreadCallback callback;
	private int successProcessedBlocks = 0;
	private int blockSize              = 1;
	private int head                   = 0;
	private int offset                 = 0;

	public ServerThread(final ServerAPI server, final ResultsContainer container, final ServerThreadCallback callback) {
		this.server = server;
		this.container = container;
		this.callback = callback;
	}

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
			catch (final RemoteException e) {
				// Decrement block size
				successProcessedBlocks = 0;
				if (blockSize > 1) {
					--blockSize;
				}
			}
		}
	}

	private ArrayList<String> buildTaskBlock() {
		final ArrayList<String> taskBlock = new ArrayList<>();
		for (offset = 0; offset < blockSize && head + offset < container.size(); ++offset) {
			taskBlock.add(container.get(head + offset).getKey());
		}
		return taskBlock;
	}

	private void insert(final ArrayList<Integer> resultBlock) {
		int iRes = 0;
		for (final Integer result : resultBlock) {
			container.get(head + iRes++).getValue().add(result);
		}
	}

	private boolean checkNonNull() {
		return server != null && container != null;
	}

	private void reset() {
		successProcessedBlocks = 0;
		blockSize = 1;
		head = 0;
		offset = 0;
	}

	private void onConnectionFailure() {
		if (callback != null) {
			callback.onFailure(server);
		}
	}
}
