import java.util.ArrayList;
import java.util.List;

public class ResultsContainer extends ArrayList<ResultEntry> {

	private static final long serialVersionUID = 1L;
	
	public ResultsContainer() {
		
	}
	
	public ResultsContainer(final List<String> instructions) {
		for (final String instruction : instructions) {
			add(instruction);
		}
	}

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

	public void add(final String instruction) {
		add(new ResultEntry(instruction, new ArrayList<>()));
	}
}
