package de.gymolching.fsb.hal;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.gpio.extension.pca.PCA9685GpioProvider;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import de.gymolching.fsb.halApi.ArmInterface;

/**
 * Hardware access to a hexapod's arm.
 * @author sschaeffner
 */
public class ArmImpl implements ArmInterface {

    //max pwm value (=100%) for pwm chip
    private final static int PWM_MAX_VALUE = 24999;

    //address of pin connected to h-driver's 1A pin
    private final static Pin H_DRIVER_1A_PIN = MCP23017Pin.GPIO_B6;

    //address of pin connected to h-driver's 2A pin
    private final static Pin H_DRIVER_2A_PIN = MCP23017Pin.GPIO_B7;

    //amount of bits of the counter used
    private final static int COUNTER_BITS = 12;

    //leftover pin that was connected to the counter-chip's clock pin for debugging purposes
    private final static Pin COUNTER_CLOCK_PIN = MCP23017Pin.GPIO_B5;

    //address of pin connected to counter chip's clear pin
    private final static Pin COUNTER_CLEAR_PIN = MCP23017Pin.GPIO_B4;

    //how long the pin connected to the counter chip's clear pin should be high
    private final static int COUNTER_CLEAR_HIGH_TIME_MILLIS = 5;

    //how long should be waited after the counter chip's clear was high
    private final static int COUNTER_CLEAR_WAIT_TIME_MILLIS = 50;

    //pwm output pin connected to the h-driver's enable pin
    private final GpioPinPwmOutput hDriverEnPwmOutputPin;

    //pwm gpio provider used to set the pwm pin to off
    private PCA9685GpioProvider pwmGpioProvider;

    //speed that was set last
    private int lastSpeed;

    //how long the motor should reverse in stop() method
    private final static int STOP_REVERSE_DURATION_MILLIS = 100;

    //pin connected to h-driver's 1A pin
    private final GpioPinDigitalOutput hDriver1A;

    //pin connected to h-driver's 2A pin
    private final GpioPinDigitalOutput hDriver2A;

    //pins connected to the counter chip's outputs
    private final GpioPinDigitalInput[] counterQ;

    //pin connected to counter chip's clear pin
    private final GpioPinDigitalOutput counterClr;

    //the current direction the motor is turning in
    private int currentDirection;

    //the counter's value is added to/removed from this when cleared
    private int counterBuffer;

    //access to the counter chip is blocked while being cleared
    private boolean counterBlocked;

    /**
     * Initializes an arm.
     * @param expGpioProvider       GPIO expansion provider for the arm's counter and h-bridge
     * @param hDriverEnPwmOutputPin pwmPin to control the motor's speed
     */
    public ArmImpl(final GpioController gpio, final MCP23017GpioProvider expGpioProvider, final GpioPinPwmOutput hDriverEnPwmOutputPin) {
        this.hDriverEnPwmOutputPin = hDriverEnPwmOutputPin;

        //gets the pwm pin's gpio provider used to set the pin to off
        GpioProvider pwmGpioProvider = hDriverEnPwmOutputPin.getProvider();
        if (pwmGpioProvider instanceof PCA9685GpioProvider) {
            this.pwmGpioProvider = (PCA9685GpioProvider) pwmGpioProvider;
        } else {
            throw new IllegalArgumentException("hDriverEnPwmOutputPin must be provided by PCA9685GpioProvider");
        }

        //initializes pins connected to the H-Driver
        this.hDriver1A = gpio.provisionDigitalOutputPin(expGpioProvider, H_DRIVER_1A_PIN, H_DRIVER_1A_PIN.getName() + " (hDriver 1A)", PinState.LOW);
        this.hDriver2A = gpio.provisionDigitalOutputPin(expGpioProvider, H_DRIVER_2A_PIN, H_DRIVER_2A_PIN.getName() + " (hDriver 2A)", PinState.LOW);

        //initializes pins connected to the counter chip's output pins
        this.counterQ = new GpioPinDigitalInput[COUNTER_BITS];
        for (int i = 0; i < COUNTER_BITS; i++) {
            this.counterQ[i] = gpio.provisionDigitalInputPin(expGpioProvider, MCP23017Pin.ALL[i], MCP23017Pin.ALL[i].getName() + " (counter " + i + ")");
            if (i == COUNTER_BITS - 2) {
                this.counterQ[i].addListener(new CounterOverflowListener());
            }
        }

        //initializes others and clear counter
        this.counterClr = gpio.provisionDigitalOutputPin(expGpioProvider, COUNTER_CLEAR_PIN, COUNTER_CLEAR_PIN.getName() + " (counter clr)", PinState.LOW);
        this.currentDirection = 0;
        this.counterBuffer = 0;
        this.counterBlocked = false;
        clearCounter();
    }

    /**
     * Sets the motor's speed.
     *
     * @param percentage how fast the motor should drive
     */
    @Override
    public void setSpeed(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("percentage for setSpeed must be between 0 and 100.");
        } else {
            this.lastSpeed = (int) Math.round(PWM_MAX_VALUE * (percentage / (double) 100));
            if (this.lastSpeed != 0) {
                this.hDriverEnPwmOutputPin.setPwm(this.lastSpeed);
            }
        }
    }

    /**
     * Starts the motor driving forward.
     */
    @Override
    public void startForward() {
        this.hDriverEnPwmOutputPin.setPwm(this.lastSpeed);
        this.hDriver2A.low();
        this.hDriver1A.high();
        this.currentDirection = 1;
    }

    /**
     * Starts the motor driving backward.
     */
    @Override
    public void startBackward() {
        this.hDriverEnPwmOutputPin.setPwm(this.lastSpeed);
        this.hDriver1A.low();
        this.hDriver2A.high();
        this.currentDirection = -1;
    }

    /**
     * Stops the motor by using the H-Driver.
     * This stops the motor by changing both the H-Driver's inputs and the pwm frequency.
     * Does not change the speed.
     */
    @Override
    public void stop() {
        stop(false);
    }

    /**
     * Stops the motor by using the H-Driver.
     * This stops the motor by changing both the H-Driver's inputs and the pwm frequency.
     * Does not change the speed.
     * If reverse is true, the motor runs reverse shortly to stop it more abruptly.
     * The motor's speed for this operation remains unchanged.
     * @param reverse   whether the motor should run reverse shortly to stop more harshly
     */
    @Override
    public void stop(boolean reverse) {
        if (reverse) {
            if (this.currentDirection == 1) {
                //startBackward without setting direction!
                this.hDriverEnPwmOutputPin.setPwm(this.lastSpeed);
                this.hDriver1A.low();
                this.hDriver2A.high();
            } else if (currentDirection == -1) {
                //startForward without setting direction!
                this.hDriverEnPwmOutputPin.setPwm(this.lastSpeed);
                this.hDriver2A.low();
                this.hDriver1A.high();
            } else {
                throw new IllegalStateException("cannot reverse the motor as the motor's current direction is unclear");
            }
            try {
                Thread.sleep(STOP_REVERSE_DURATION_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.hDriver1A.high();
        this.hDriver2A.high();
        putCurrentPositionIntoBuffer();
        this.currentDirection = 0;
    }

    /**
     * Stops the motor by using pwm.
     * This sets the pwm frequency to 0 but does not change the H-Driver's inputs.
     */
    @Override
    public void stopByPwm() {
        this.pwmGpioProvider.setAlwaysOff(this.hDriverEnPwmOutputPin.getPin());
        putCurrentPositionIntoBuffer();
        this.currentDirection = 0;
    }

    /**
     * Returns the position of the motor as counted by the counter.
     *
     * @return the position of the motor
     */
    @Override
    public int getPosition() {
        int counterInput = readCurrentCounterValue() * this.currentDirection;
        return this.counterBuffer + counterInput;
    }

    /**
     * Moves the motor into starting position.
     * This is a blocking method.
     */
    @Override
    public void moveToStartingPosition() {
        int latestSpeedS = this.lastSpeed;
        setSpeed(100);
        startBackward();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stop();
        resetPositionBuffer();

        if (latestSpeedS != 0) this.hDriverEnPwmOutputPin.setPwm(latestSpeedS);
    }

    /**
     * Reads the counter chip's value and adds it to the internal buffer. Then clears the counter chip's value.
     */
    private void putCurrentPositionIntoBuffer() {
        this.counterBuffer = getPosition();
        clearCounter();
    }

    /**
     * Resets position counter.
     * This should be called when the arm has reached its starting position.
     */
    @Override
    public void resetPositionBuffer() {
        this.counterBuffer = 0;
        clearCounter();
    }

    /**
     * Reads the counter chip's current value
     * @return  counter chip's value
     */
    private int readCurrentCounterValue() {
        if (!this.counterBlocked) {
            String ins = "";
            for (int i = this.counterQ.length - 1; i >= 0; i--) {
                ins += (this.counterQ[i].getState().isHigh()) ? "1" : "0";
            }
            return Integer.valueOf(ins, 2);
        } else {
            return 0;
        }
    }

    /**
     * Clears the counter chip's value.
     */
    private void clearCounter() {
        this.counterBlocked = true;
        this.counterClr.high();
        try {
            Thread.sleep(COUNTER_CLEAR_HIGH_TIME_MILLIS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.counterClr.low();
        try {
            Thread.sleep(COUNTER_CLEAR_WAIT_TIME_MILLIS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.counterBlocked = false;
    }

    /**
     * Listener waiting for the counter chip to overflow.
     * Clears the counter chip when close to overflowing.
     */
    private class CounterOverflowListener implements GpioPinListenerDigital {

        /**
         * Clears the counter chip when close to overflowing.
         */
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpioPinDigitalStateChangeEvent) {
            clearCounter();
            counterBuffer += Math.pow(2, COUNTER_BITS - 2) * currentDirection;
        }
    }
}
