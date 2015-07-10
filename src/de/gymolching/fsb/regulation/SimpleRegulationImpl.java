package de.gymolching.fsb.regulation;

import de.gymolching.fsb.Launcher;
import de.gymolching.fsb.api.FSBPosition;
import de.gymolching.fsb.halApi.ArmInterface;

import java.util.ArrayList;

/**
 * @author sschaeffner
 */
public class SimpleRegulationImpl implements RegulationInterface, Runnable {

    //how many steps are available to 100%
    private static final int MAX_STEPS = 37;

    //how long to wait between each position poll
    private static final int POLLING_RATE_TIME_MILLIS = 100;

    //position provided (FSBServer)
    private PositionProvider positionProvider;

    //array of arms
    private final ArmInterface[] arms;

    //one thread per arm
    private final Thread[] armThreads;

    //current goal lengths for every arm
    private final int[] lengths;

    //whether an arm is currently moving
    private final boolean[] armMoving;

    //main watch thread
    private final Thread mainWatchThread;

    public SimpleRegulationImpl(ArmInterface[] arms) {
        this.arms = arms;

        lengths = new int[6];

        armMoving = new boolean[arms.length];
        for (int i = 0; i < arms.length; i++) {
            armMoving[i] = true;
        }

        this.armThreads = new Thread[arms.length];
        for (int i = 0; i < this.armThreads.length; i++) {
            this.armThreads[i] = new Thread(new ArmThread(arms[i], i));
            this.armThreads[i].start();
        }

        mainWatchThread = new Thread(this);
        mainWatchThread.start();
    }

    @Override
    public void setPositionProvider(PositionProvider positionProvider) {
        this.positionProvider = positionProvider;
    }

    //Main watch thread
    @Override
    public void run() {

        //wait for all arms to be done moving to starting position
        boolean allAtStartingPosition = false;
        while (!allAtStartingPosition) {
            allAtStartingPosition = true;
            for (int i = 0; i < this.armMoving.length; i++) {
                if (this.armMoving[i]) allAtStartingPosition = false;
            }
            if (!allAtStartingPosition) {
                synchronized (this.armMoving) {
                    try {
                        this.armMoving.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("[MWT] change in armMoving");
            }
        }

        System.out.println("[MWT] all arms at starting position");

        while (Launcher.isRunning()) {

            System.out.println("[MWT] waiting for new position...");

            //get most recent position
            FSBPosition position = null;
            while (position == null) {
                try {
                    position = positionProvider.getMostRecentPositionUpdate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("[MWT] new position: " + position.toString());

            //set lengths
            lengths[0] = (int) Math.round(((double) position.getLength1() / (double) FSBPosition.MAX) * (double) MAX_STEPS);
            lengths[1] = (int) Math.round(((double) position.getLength2() / (double) FSBPosition.MAX) * (double) MAX_STEPS);
            lengths[2] = (int) Math.round(((double) position.getLength3() / (double) FSBPosition.MAX) * (double) MAX_STEPS);
            lengths[3] = (int) Math.round(((double) position.getLength4() / (double) FSBPosition.MAX) * (double) MAX_STEPS);
            lengths[4] = (int) Math.round(((double) position.getLength5() / (double) FSBPosition.MAX) * (double) MAX_STEPS);
            lengths[5] = (int) Math.round(((double) position.getLength6() / (double) FSBPosition.MAX) * (double) MAX_STEPS);

            System.out.println("[MWT] received new position");

            //set armMoving to true, notify ArmThreads
            for (int i = 0; i < armMoving.length; i++) armMoving[i] = true;

            synchronized (this.lengths) {
                this.lengths.notifyAll();
            }

            System.out.println("DEB notified");

            //wait for all arms to be done moving
            boolean allDone = false;
            while (!allDone) {
                allDone = true;
                for (boolean anArmMoving : this.armMoving) {
                    if (anArmMoving) allDone = false;
                }
                if (!allDone) {
                    synchronized (this.armMoving) {
                        try {
                            this.armMoving.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("[MWT] change in armMoving");
            }
        }
    }

    /**
     * One thread an arm.
     */
    private class ArmThread implements Runnable {
        private final ArmInterface arm;
        private final int armId;

        private ArmThread(ArmInterface arm, int armId) {
            this.arm = arm;
            this.armId = armId;
        }

        @Override
        public void run() {
            //drive to starting position
            System.out.println("[ARM" + armId + "] moving to starting position");
            arm.moveToStartingPosition();
            armMoving[armId] = false;
            System.out.println("[ARM" + armId + "] at starting position");

            synchronized (armMoving) {
                armMoving.notifyAll();
            }

            while (Launcher.isRunning()) {

                synchronized (lengths) {
                    try {
                        lengths.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("[ARM" + armId + "] received new position");

                if (lengths != null) {
                    //drive to new position
                    int currentPos = arm.getPosition();
                    int goalPos = lengths[armId];
                    if (currentPos > goalPos) {
                        arm.setSpeed(100);
                        arm.startBackward();
                        while (currentPos > goalPos) {
                            try {
                                Thread.sleep(POLLING_RATE_TIME_MILLIS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentPos = arm.getPosition();
                        }
                    } else if (currentPos < goalPos) {
                        arm.setSpeed(100);
                        arm.startForward();
                        while (currentPos < goalPos) {
                            try {
                                Thread.sleep(POLLING_RATE_TIME_MILLIS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentPos = arm.getPosition();
                        }
                    }
                    arm.stop();
                    armMoving[armId] = false;
                    System.out.println("[ARM" + armId + "] at goal position");
                    synchronized (armMoving) {
                        armMoving.notifyAll();
                    }
                }
            }
        }
    }
}
