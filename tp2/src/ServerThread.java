import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

public class ServerThread extends Thread {

	final static int SUCCESS_BLOCK_INCREMENT = 5;

	final ServerAPI server;
	final ArrayList<Map.Entry<String, ArrayList<Integer>>> container;
	final ServerThreadCallback callback;
	int successProcessedBlocks = 0;
	int blockSize              = 1;
	int head                   = 0;
	int offset                 = 0;

	public ServerThread(
			ServerAPI server,
			ArrayList<Map.Entry<String, ArrayList<Integer>>> container,
			ServerThreadCallback callback) {
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
			e.printStackTrace();
			if (callback != null) {
				callback.onFailure(server);
			}
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
}
