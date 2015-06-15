package de.gymolching.fsb.server.api;

import de.gymolching.fsb.api.FSBPosition;

public interface FSBServerInterface
{
	/**
	 * Blocking method. Shuts the server down
	 * 
	 * @throws InterruptedException
	 */
	public void stop() throws InterruptedException;

	/**
	 * returns the most recent received FSBPosition. If no positions are in store, this method will block
	 * 
	 * @throws InterruptedException
	 *             when the blocking is interrupted
	 * @return
	 */
	public FSBPosition getMostRecentPositionUpdate() throws InterruptedException;
}
