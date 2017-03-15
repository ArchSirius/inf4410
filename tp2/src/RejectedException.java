/**
 * Custom Exception for server tasks rejection.
 *
 */
public class RejectedException extends Exception {

	private static final long serialVersionUID = 2L;

	/**
	 * Message constructor
	 * @param message An informative message.
	 */
	public RejectedException(final String message) {
		super(message);
	}
}
