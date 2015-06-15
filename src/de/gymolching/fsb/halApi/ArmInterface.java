package de.gymolching.fsb.halApi;

/**
 * Hardware access to a hexapod's arm.
 */
public interface ArmInterface {

    /**
     * Sets the motor's speed.
     * @param percentage how fast the motor should drive
     */
    void setSpeed(int percentage);

    /**
     * Starts the motor driving forward.
     */
    void startForward();

    /**
     * Starts the motor driving backward.
     */
    void startBackward();

    /**
     * Stops the motor by using the H-Driver.
     * This stops the motor by changing both the H-Driver's inputs and the pwm frequency.
     * Does not change the speed.
     */
    void stop();

    /**
     * Stops the motor by using the H-Driver.
     * This stops the motor by changing both the H-Driver's inputs and the pwm frequency.
     * Does not change the speed.
     * @param reverse   whether the motor should run reverse shortly to stop more harshly
     */
    void stop(boolean reverse);

    /**
     * Stops the motor by using pwm.
     * This sets the pwm frequency to 0 but does not change the H-Driver's inputs.
     */
    void stopByPwm();

    /**
     * Returns the position of the motor as counted by the counter.
     * @return the position of the motor
     */
    int getPosition();

    /**
     * Moves the motor into starting position.
     * This is a blocking method.
     */
    void moveToStartingPosition();

    /**
     * Resets position counter.
     * This should be called when the arm has reached its starting position.
     */
    void resetPositionBuffer();
}
