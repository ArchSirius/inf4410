package LoadBalancer;
import java.util.ArrayList;
import java.util.Map;

/**
 * A tuple of instruction-results.
 *
 */
public class ResultEntry implements Map.Entry<String, ArrayList<Integer>> {

	private final String instruction;
	private ArrayList<Integer> results;

	/**
	 * Constructor.
	 * @param instruction An instruction.
	 * @param results An ArrayList of computed results.
	 */
	public ResultEntry(final String instruction, final ArrayList<Integer> results) {
		this.instruction = instruction;
		this.results = results;
	}

	/**
	 * Returns the instruction.
	 * @return The instruction.
	 */
	@Override
	public String getKey() {
		return instruction;
	}

	/**
	 * Returns the results.
	 * @return The results.
	 */
	@Override
	public ArrayList<Integer> getValue() {
		return results;
	}

	/**
	 * Sets the results.
	 * @param results The new results.
	 * @return The old results.
	 */
	@Override
	public ArrayList<Integer> setValue(ArrayList<Integer> results) {
		final ArrayList<Integer> oldResults = results;
		this.results = results;
		return oldResults;
	}

	/**
	 * Adds a result to the results.
	 * @param result A result to add to the results.
	 * @return true (as specified by Collection.add(E)).
	 */
	public boolean add(final Integer result) {
		if (results == null) {
			results = new ArrayList<>();
		}
		return results.add(result);
	}
}
