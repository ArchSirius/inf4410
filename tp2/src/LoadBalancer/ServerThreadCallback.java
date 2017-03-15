package LoadBalancer;
import Server.ServerAPI;

/**
 * A callback interface implemented by ServerThread users.
 *
 */
public interface ServerThreadCallback {

	/**
	 * Callback on server failure.
	 * @param server The server that failed.
	 */
	public void onFailure(ServerAPI server);
}
