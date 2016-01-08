package de.gymolching.fsb.regulation;

public interface RegulationManagerInterface {
	/**
	 * retrieves the current arm length for armId
	 * 
	 * @param armId
	 * @return
	 */
	public int getArmLength(int armId);

	/**
	 * Sets the length of one arm
	 * 
	 * @param armId
	 * @param newLength
	 */
	public void setArmLength(int armId, int newLength);

	/**
	 * Receives "armMoving" status for arm @ armID
	 * 
	 * @param armId
	 * @return
	 */
	public boolean isArmMoving(int armId);
}
