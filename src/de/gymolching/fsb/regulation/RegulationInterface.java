package de.gymolching.fsb.regulation;

import de.gymolching.fsb.api.FSBPosition;

/**
 * @author sschaeffner
 */
public interface RegulationInterface {
    void onPositionUpdate(FSBPosition position);
}
