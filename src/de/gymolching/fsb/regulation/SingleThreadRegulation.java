package de.gymolching.fsb.regulation;

import de.gymolching.fsb.Launcher;
import de.gymolching.fsb.api.FSBPosition;
import de.gymolching.fsb.halApi.ArmInterface;

import java.util.logging.Logger;

/**
 * @author sschaeffner
 */
public class SingleThreadRegulation implements RegulationInterface {

    //amount of arms available
    private final int ARM_AMOUNT = 6;

    //amount of steps from 0% to 100%
    private final int MAX_STEPS = 37;

    //Logging
    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    //PositionProvider instance
    private PositionProvider positionProvider;

    //all 6 arms
    private final ArmInterface[] arms;

    //main thread instance
    private Thread mainThread;


    public SingleThreadRegulation(ArmInterface[] arms) {
        this.arms = arms;
        setupMainThread();
    }

    private void setupMainThread() {
        mainThread = new Thread(() -> {
            //move to starting position
            for (int i = 0; i < ARM_AMOUNT; i++) {
                arms[i].moveToStartingPosition();
            }

            while (Launcher.isRunning()) {
                if (positionProvider != null) {

                    //pull position from client (network)
                    FSBPosition position = null;
                    try {
                        position = positionProvider.getMostRecentPositionUpdate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (position == null) {
                        Launcher.exit();
                    } else {

                        //put position into easily accessible array
                        int[] armLengths = new int[ARM_AMOUNT];

                        armLengths[0] = (int) Math.round(((double) position.getLength1() / (double) FSBPosition.MAX) * (double) arms[0].getMaxPosition());
                        armLengths[1] = (int) Math.round(((double) position.getLength2() / (double) FSBPosition.MAX) * (double) arms[1].getMaxPosition());
                        armLengths[2] = (int) Math.round(((double) position.getLength3() / (double) FSBPosition.MAX) * (double) arms[2].getMaxPosition());
                        armLengths[3] = (int) Math.round(((double) position.getLength4() / (double) FSBPosition.MAX) * (double) arms[3].getMaxPosition());
                        armLengths[4] = (int) Math.round(((double) position.getLength5() / (double) FSBPosition.MAX) * (double) arms[4].getMaxPosition());
                        armLengths[5] = (int) Math.round(((double) position.getLength6() / (double) FSBPosition.MAX) * (double) arms[5].getMaxPosition());

                        //start each arm to move into the right direction
                        for (int i = 0; i < ARM_AMOUNT; i++) {
                            if (arms[i].getPosition() < armLengths[i]) {
                                arms[i].setSpeed(100);
                                arms[i].startForward();
                            } else {
                                arms[i].setSpeed(100);
                                arms[i].startBackward();
                            }
                        }

                        //loop until all arms are arrived at the desired position
                        boolean[] arrived = new boolean[ARM_AMOUNT];
                        while (!allTrue(arrived)) {

                            //check each arm's position if it reached its desired position
                            for (int i = 0; i < ARM_AMOUNT; i++) {
                                if (arms[i].getDirection() == 1) {
                                    //if currently moving forward, check if position is greater/equal than desired position
                                    if (arms[i].getPosition() >= armLengths[i]) {
                                        //then stop
                                        arms[i].stop();
                                        arrived[i] = true;
                                    }//else do nothing (go on moving)
                                } else if (arms[i].getDirection() == -1) {
                                    //if currently moving backward, check if position is smaller/equal than desired position
                                    if (arms[i].getPosition() <= armLengths[i]) {
                                        //then stop
                                        arms[i].stop();
                                        arrived[i] = true;
                                    }//else do nothing (go on moving)
                                } else if (arms[i].getDirection() == 0) {
                                    //if arm is not moving at all, do nothing
                                }
                            }
                        }

                        log.fine("all arms arrived at desired position");
                    }
                } else {
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mainThread.start();
    }

    private static boolean allTrue(boolean[] bools) {
        for (boolean b : bools) if (!b) return false;
        return true;
    }

    @Override
    public void setPositionProvider(PositionProvider positionProvider) {
        this.positionProvider = positionProvider;
    }
}
