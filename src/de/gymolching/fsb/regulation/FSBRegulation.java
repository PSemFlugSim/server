package de.gymolching.fsb.regulation;

import de.gymolching.fsb.halApi.ArmInterface;
import de.gymolching.fsb.network.api.FSBServerInterface;

/**
 * @author sschaeffner
 */
public abstract class FSBRegulation {
	// All 6 arms
	protected ArmInterface[] arms;
	
	// Position provider instance
	protected FSBServerInterface positionProvider;

	public FSBRegulation(ArmInterface[] arms, FSBServerInterface positionProvider) {
		this.arms = arms;
		this.positionProvider = positionProvider;
	}
}
