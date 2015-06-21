package de.gymolching.fsb.regulation;

import de.gymolching.fsb.api.FSBPosition;

/**
 * @author sschaeffner
 */
public interface PositionProvider {
    /**
     * returns the most recent received FSBPosition. If no positions are in store, this method will block
     *
     * @throws InterruptedException when the blocking is interrupted
     * @return the most recent position
     */
    public FSBPosition getMostRecentPositionUpdate() throws InterruptedException;
}
