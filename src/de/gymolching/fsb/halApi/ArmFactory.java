package de.gymolching.fsb.halApi;

import java.io.IOException;

/**
 * @author sschaeffner
 */
public interface ArmFactory {
    /**
     * Provides an instance of ArmInterface for the given nr.
     * @param nr            number of arm to get instance of
     * @return              instance of ArmInterface for nr
     * @throws IOException  if hardware (like i2c port) is not available
     */
    ArmInterface provideArm(int nr) throws IOException;
}
