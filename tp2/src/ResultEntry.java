import java.util.ArrayList;
import java.util.Map;

public class ResultEntry implements Map.Entry<String, ArrayList<Integer>> {

	private final String instruction;
	private ArrayList<Integer> results;

	public ResultEntry(final String instruction, final ArrayList<Integer> results) {
		this.instruction = instruction;
		this.results = results;
	}

	@Override
	public String getKey() {
		return instruction;
	}

	@Override
	public ArrayList<Integer> getValue() {
		return results;
	}

	@Override
	public ArrayList<Integer> setValue(ArrayList<Integer> results) {
		final ArrayList<Integer> oldResults = results;
		this.results = results;
		return oldResults;
	}

	public boolean add(final Integer result) {
		if (results == null) {
			results = new ArrayList<>();
		}
		results.add(result);
		return true;
	}
}
