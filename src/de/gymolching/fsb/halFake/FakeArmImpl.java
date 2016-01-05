package de.gymolching.fsb.halFake;

import de.gymolching.fsb.Launcher;
import de.gymolching.fsb.halApi.ArmInterface;

/**
 * @author sschaeffner
 */
public class FakeArmImpl implements ArmInterface {

    private final PositionChangeListener listener;
    private final Thread moverThread;
    private final int maxAbsPos;
    private int currentAbsPos;
    private int speedPerc;
    private int currentDirection;

    public FakeArmImpl(PositionChangeListener listener) {
        this.listener = listener;
        maxAbsPos = 1024;
        currentAbsPos = 10;

        speedPerc = 0;
        currentDirection = 0;

        moverThread = new Thread(() -> {
            while (Launcher.isRunning()) {
                if (currentDirection != 0) {
                    currentAbsPos += (speedPerc / 10) * currentDirection;
                    if (currentAbsPos < 0) {
                        currentAbsPos = 0;
                    } else if (currentAbsPos > maxAbsPos) {
                        currentAbsPos = maxAbsPos;
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        moverThread.start();
    }

    /**
     * Sets the motor's speed.
     *
     * @param percentage how fast the motor should drive
     */
    @Override
    public void setSpeed(int percentage) {
        this.speedPerc = percentage;
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * Starts the motor driving forward.
     */
    @Override
    public void startForward() {
        this.currentDirection = 1;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Starts the motor driving backward.
     */
    @Override
    public void startBackward() {
        this.currentDirection = -1;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Stops the motor by using the H-Driver.
     * This stops the motor by changing both the H-Driver's inputs and the pwm frequency.
     * Does not change the speed.
     */
    @Override
    public void stop() {
        this.currentDirection = 0;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Stops the motor by using the H-Driver.
     * This stops the motor by changing both the H-Driver's inputs and the pwm frequency.
     * Does not change the speed.
     *
     * @param reverse whether the motor should run reverse shortly to stop more harshly
     */
    @Override
    public void stop(boolean reverse) {
        this.currentDirection = 0;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Stops the motor by using pwm.
     * This sets the pwm frequency to 0 but does not change the H-Driver's inputs.
     */
    @Override
    public void stopByPwm() {
        this.currentDirection = 0;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Returns the absolute position of the motor
     *
     * @return the position of the motor
     */
    @Override
    public int getPosition() {
        return currentAbsPos;
    }

    /**
     * Moves the motor into starting position.
     * This is a blocking method.
     */
    @Override
    public void moveToStartingPosition() {
        this.currentAbsPos = 0;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Resets position counter.
     * This should be called when the arm has reached its starting position.
     */
    @Override
    public void resetPositionBuffer() {
        throw new IllegalStateException("cannot reset position buffer on FakeArmImpl");
    }

    public int getCurrentAbsPos() {
        return currentAbsPos;
    }

    public int getMaxAbsPos() {
        return maxAbsPos;
    }

    interface PositionChangeListener {
        void onPositionChange();
    }
}
