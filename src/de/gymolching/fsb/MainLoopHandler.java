package de.gymolching.fsb;

import com.pi4j.io.gpio.GpioFactory;
import de.gymolching.fsb.hal.ArmFactoryImpl;
import de.gymolching.fsb.halApi.ArmFactory;
import de.gymolching.fsb.halApi.ArmInterface;
import de.gymolching.fsb.halFake.FakeArmFactory;
import de.gymolching.fsb.network.api.FSBServerInterface;
import de.gymolching.fsb.network.implementation.FSBServer;
import de.gymolching.fsb.regulation.PositionProvider;
import de.gymolching.fsb.regulation.RegulationInterface;
import de.gymolching.fsb.regulation.SimpleRegulationImpl;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Handles the main loop connecting server, regulation and hal.
 * @author sschaeffner
 */
public class MainLoopHandler {

    //Logging
    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    //amount of arms of the hexapod
    private static final int ARM_AMOUNT = 6;

    //port of the network server
    private static final int SERVER_PORT = 1234;

    //singleton instance of MainLoopHandler
    private static MainLoopHandler instance = null;

    //network server
    private FSBServerInterface server;

    //regulation
    private RegulationInterface regulationInterface;

    //hal arm factory
    private ArmFactory armFactory;

    //array of arms
    private ArmInterface[] arms;

    private MainLoopHandler() {
        try {
            this.server = new FSBServer(SERVER_PORT, false);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        if (Launcher.isFakeMode()) {
            this.armFactory = FakeArmFactory.getInstance();
        } else {
            this.armFactory = ArmFactoryImpl.getInstance(GpioFactory.getInstance());
        }

        this.arms = new ArmInterface[ARM_AMOUNT];

        for (int i = 0; i < ARM_AMOUNT; i++) {
            try {
                this.arms[i] = armFactory.provideArm(i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        regulationInterface = new SimpleRegulationImpl(arms);
    }


    /**
     * Starts the main loop.
     */
    public void mainLoop() {
        if (server instanceof PositionProvider) {
            regulationInterface.setPositionProvider((PositionProvider) server);
        } else {
            throw new IllegalStateException("FSBServer has to be a PositionProvider!");
        }
    }

    /**
     * Returns an instance of MainLoopHandler
     * @return instance of MainLoopHandler
     */
    public static MainLoopHandler getInstance() {
        synchronized (MainLoopHandler.class) {
            if (instance == null) instance = new MainLoopHandler();
        }
        return instance;
    }
}
