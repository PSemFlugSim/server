package de.gymolching.fsb.hal;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.pca.PCA9685GpioProvider;
import com.pi4j.gpio.extension.pca.PCA9685Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import de.gymolching.fsb.halApi.ArmInterface;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Creates instances of hexapod arms.
 * @author sschaeffner
 */
public class ArmFactory {

    //singleton instance of ArmFactory
    private static ArmFactory instance;

    /**
     * Returns an instance of an ArmFactory to provide arms.
     * @param gpioController    a GpioController
     * @return instance of an ArmFactory to provide arms.
     */
    public static ArmFactory getInstance(final GpioController gpioController) {
        if (instance == null) instance = new ArmFactory(gpioController);
        return instance;
    }

    //i2c bus id
    private static final int I2CBUS_ID = I2CBus.BUS_1;

    //the amount of arms available
    private static final int NR_OF_ARMS_AVAILABLE = 6;

    //i2c base address of mcp gpio expander chip
    private static final byte MCP_BASE_ADDR = 0x20;

    //i2c base address of pca pwm chip
    private static final byte PCA_BASE_ADDR = 0x40;

    //pwm target frequency for pca chip
    private static final BigDecimal PCA_TARGET_FREQUENCY = new BigDecimal(40);

    //gpio controller from pi4j
    private final GpioController gpio;

    //i2c bus instance of I2CBUS_ID
    private I2CBus i2cBus;

    //gpio provider instance of pca pwm chip
    private PCA9685GpioProvider pwmGpioProvider;

    /**
     * Initializes an ArmFactory with a GpioController.
     * @param gpio an instance of a pi4j GpioController
     */
    private ArmFactory(final GpioController gpio) {
        this.gpio = gpio;

        try {
            this.i2cBus = I2CFactory.getInstance(I2CBUS_ID);
            this.pwmGpioProvider = new PCA9685GpioProvider(i2cBus, PCA_BASE_ADDR, PCA_TARGET_FREQUENCY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Provides an arm with a given port nr.
     * @param nr    which arm to provide
     * @return      provided arm
     * @throws IOException when i2c bus is not initialized
     */
    public ArmInterface provideArm(int nr) throws IOException {
        if (i2cBus == null) {
            throw new IOException("i2c bus not initialized. Cannot provide arm.");
        }

        if (nr < 0 || nr > NR_OF_ARMS_AVAILABLE) {
            throw new IllegalArgumentException("there are only " + NR_OF_ARMS_AVAILABLE + " arms available. nr must be between 0 and " + (NR_OF_ARMS_AVAILABLE - 1) + ".");
        }

        MCP23017GpioProvider expGpioProvider = new MCP23017GpioProvider(i2cBus, MCP_BASE_ADDR + nr);
        GpioPinPwmOutput hBridgeEnPwmOutputPin = GpioFactory.getInstance().provisionPwmOutputPin(pwmGpioProvider, PCA9685Pin.ALL[nr]);

        return new ArmImpl(gpio, expGpioProvider, hBridgeEnPwmOutputPin);
    }
}
