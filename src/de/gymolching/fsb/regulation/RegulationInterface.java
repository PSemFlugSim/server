package de.gymolching.fsb.regulation;

import de.gymolching.fsb.network.api.FSBServerInterface;

/**
 * @author sschaeffner
 */
public interface RegulationInterface {
	void setPositionProvider(FSBServerInterface positionProvider);
}
