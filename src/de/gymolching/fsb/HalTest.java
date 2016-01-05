package de.gymolching.fsb;

import com.pi4j.io.gpio.GpioFactory;
import de.gymolching.fsb.hal.ArmFactoryImpl;
import de.gymolching.fsb.halApi.ArmFactory;
import de.gymolching.fsb.halApi.ArmInterface;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author sschaeffner
 */
public class HalTest {

    //Logging
    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    //amount of arms of the hexapod
    private static final int ARM_AMOUNT = 6;

    //hal arm factory
    private ArmFactory armFactory;

    //array of arms
    private ArmInterface[] arms;


    public HalTest() {
        this.armFactory = ArmFactoryImpl.getInstance(GpioFactory.getInstance());


        this.arms = new ArmInterface[ARM_AMOUNT];

        /*for (int i = 0; i < ARM_AMOUNT; i++) {
            try {
                this.arms[i] = armFactory.provideArm(i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        try {
            this.arms[4] = armFactory.provideArm(4);
            this.arms[5] = armFactory.provideArm(5);
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("HAL TEST initialized");
    }

    /**
     * Interprets a command.
     *
     * @param cmd   command to interpret
     * @return      whether the command was executed
     */
    public boolean interpretCommand(String cmd) {
        String[] cmdParts = cmd.split(" ");

        if (cmdParts[0].equalsIgnoreCase("hal")) {
            if (cmdParts[1].equalsIgnoreCase("help")) {
                System.out.println("HAL TEST");
                System.out.println("hal [nr] start [f/b]  starts motor [nr] [f/b]");
                System.out.println("hal [nr] stop         stops motor [nr]");
                System.out.println("hal [nr] speed [s]    sets motor [nr] to speed [s]");
                System.out.println("hal [nr] pos          returns position of arm [nr]");
                System.out.println("hal [nr] reset        resets position buffer of arm [nr]");
                System.out.println("hal help              prints this help");
            } else if (cmdParts.length >= 3) {
                int armNr = -1;
                try {
                    armNr = Integer.valueOf(cmdParts[1]);
                } catch (NumberFormatException e) {
                    System.err.println("arm nr not a number");
                }

                if (armNr >= 0 && armNr < ARM_AMOUNT) {
                    switch (cmdParts[2]) {
                        case "start":
                            if (cmdParts.length >= 4) {
                                if (cmdParts[3].equalsIgnoreCase("f")) {
                                    System.out.println("starting fwd arm " + armNr);
                                    startFwd(armNr);
                                } else if (cmdParts[3].equalsIgnoreCase("b")) {
                                    System.out.println("starting bwd arm " + armNr);
                                    startBwd(armNr);
                                } else {
                                    System.err.println("could not recognize direction \"" + cmdParts[3] + "\"");
                                }
                            } else {
                                System.out.println("default starting fwd arm " + armNr);
                                startFwd(armNr);
                            }
                            break;

                        case "stop":
                            System.out.println("stopping arm " + armNr);
                            stop(armNr);
                            break;

                        case "speed":
                            if (cmdParts.length >= 4) {
                                int speed = -1;
                                try {
                                    speed = Integer.valueOf(cmdParts[3]);
                                } catch (NumberFormatException e) {
                                    System.err.println("speed not a number");
                                }

                                if (speed >= 0 && speed <= 100) {
                                    setSpeed(armNr, speed);
                                } else {
                                    System.err.println("speed has to be between 0 and 100");
                                }
                            }
                            break;

                        case "pos":
                            printPosition(armNr);
                            break;

                        case "reset":
                            System.out.println("resetting arm " + armNr);
                            reset(armNr);
                            break;

                        default:
                            System.err.println("could not recognize command \"" + cmdParts[2] + "\" for arm nr. " + armNr);
                            break;
                    }
                } else {
                    System.err.println("arm nr not recognized");
                }
            }
        }

        return false;
    }

    private void startFwd(int nr) {
        if (arms[nr] == null) {
            System.err.println("Arm " + nr + " not intialized");
            return;
        }
        arms[nr].startForward();
    }

    private void startBwd(int nr) {
        if (arms[nr] == null) {
            System.err.println("Arm " + nr + " not intialized");
            return;
        }
        arms[nr].startBackward();
    }

    private void stop(int nr) {
        if (arms[nr] == null) {
            System.err.println("Arm " + nr + " not intialized");
            return;
        }
        arms[nr].stop();
    }

    private void setSpeed(int nr, int percentage) {
        if (arms[nr] == null) {
            System.err.println("Arm " + nr + " not intialized");
            return;
        }
        arms[nr].setSpeed(percentage);
    }

    private void printPosition(int nr) {
        if (arms[nr] == null) {
            System.err.println("Arm " + nr + " not intialized");
            return;
        }
        int pos = arms[nr].getPosition();
        System.out.println("position of arm " + nr + ": " + pos);
    }

    private void reset(int nr) {
        if (arms[nr] == null) {
            System.err.println("Arm " + nr + " not intialized");
            return;
        }
        arms[nr].resetPositionBuffer();
    }
}
