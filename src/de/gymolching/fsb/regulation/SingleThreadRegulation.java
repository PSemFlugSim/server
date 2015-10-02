package de.gymolching.fsb.regulation;

import de.gymolching.fsb.Launcher;
import de.gymolching.fsb.api.FSBPosition;
import de.gymolching.fsb.halApi.ArmInterface;

import java.util.Arrays;
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

                        //calculate arm position diffs and then their speeds
                        int[] armPositionDiffs = new int[ARM_AMOUNT];
                        armPositionDiffs[0] = Math.abs(arms[0].getPosition() - armLengths[0]);
                        armPositionDiffs[1] = Math.abs(arms[1].getPosition() - armLengths[1]);
                        armPositionDiffs[2] = Math.abs(arms[2].getPosition() - armLengths[2]);
                        armPositionDiffs[3] = Math.abs(arms[3].getPosition() - armLengths[3]);
                        armPositionDiffs[4] = Math.abs(arms[4].getPosition() - armLengths[4]);
                        armPositionDiffs[5] = Math.abs(arms[5].getPosition() - armLengths[5]);
                        for (int i = 0; i < armPositionDiffs.length; i++) {
                            log.fine("POS DIFF " + i + ": " + armPositionDiffs[i]);
                        }

                        //get longest position diff to assign 100% speed to
                        int longestDiff = Arrays.stream(armPositionDiffs).max().getAsInt();
                        log.fine("longestDiff=" + longestDiff);

                        int[] armSpeeds = new int[ARM_AMOUNT];
                        armSpeeds[0] = (int)Math.round(((double)armPositionDiffs[0] / (double)longestDiff) * 100);
                        armSpeeds[1] = (int)Math.round(((double)armPositionDiffs[1] / (double)longestDiff) * 100);
                        armSpeeds[2] = (int)Math.round(((double)armPositionDiffs[2] / (double)longestDiff) * 100);
                        armSpeeds[3] = (int)Math.round(((double)armPositionDiffs[3] / (double)longestDiff) * 100);
                        armSpeeds[4] = (int)Math.round(((double)armPositionDiffs[4] / (double)longestDiff) * 100);
                        armSpeeds[5] = (int)Math.round(((double)armPositionDiffs[5] / (double)longestDiff) * 100);

                        for (int i = 0; i < armSpeeds.length; i++) {
                            log.fine("ARM SPEED " + i + ": " + armSpeeds[i]);
                        }


                        //start each arm to move into the right direction
                        for (int i = 0; i < ARM_AMOUNT; i++) {
                            if (arms[i].getPosition() < armLengths[i]) {
                                arms[i].setSpeed(armSpeeds[i]);
                                arms[i].startForward();
                            } else {
                                arms[i].setSpeed(armSpeeds[i]);
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
