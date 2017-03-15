import java.util.ArrayList;

/**
 * An ArrayList of instruction-result tuples.
 *
 */
public class ResultsContainer extends ArrayList<ResultEntry> {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Default constructor.
	 */
	public ResultsContainer() {
		
	}
	
	/**
	 * Constructor with instructions initialization.
	 * @param instructions An ArrayList of instructions to initialize the container.
	 */
	public ResultsContainer(final ArrayList<String> instructions) {
		for (final String instruction : instructions) {
			add(instruction);
		}
	}

	/**
	 * Returns a subset of this container by copy.
	 * @param fromIndex The index from which to begin the subset (inclusive).
	 * @param toIndex The index to which end the subset (exclusive).
	 * @return A subset of this container.
	 * @throws IndexOutOfBoundsException If an endpoint index value is out of range (fromIndex < 0 || toIndex > size).
	 * @throws IllegalArgumentException If the endpoint indices are out of order (fromIndex > toIndex).
	 */
	@Override
	public ResultsContainer subList(final int fromIndex, final int toIndex)
			throws IndexOutOfBoundsException, IllegalArgumentException {
		if (fromIndex < 0 || toIndex > size()) {
			throw new IndexOutOfBoundsException();
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException();
		}
		final ResultsContainer newContainer = new ResultsContainer();
		for (int index = fromIndex; index < toIndex; ++index) {
			newContainer.add(get(index));
		}
		return newContainer;
	}

	/**
	 * Inserts an instruction to this container with initialized empty results.
	 * @param instruction The instruction to insert.
	 */
	public void add(final String instruction) {
		add(new ResultEntry(instruction, new ArrayList<>()));
	}
}
