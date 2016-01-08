package de.gymolching.fsb.regulation;

import de.gymolching.fsb.Launcher;
import de.gymolching.fsb.api.FSBPosition;
import de.gymolching.fsb.halApi.ArmInterface;
import de.gymolching.fsb.network.api.FSBServerInterface;

/**
 * @author sschaeffner
 */
public class SimpleRegulationImpl extends FSBRegulation implements Runnable, RegulationManagerInterface {

	// how many steps are available to 100%
	public static final int MAX_STEPS = 37;

	// how long to wait between each position poll
	public static final int POLLING_RATE_TIME_MILLIS = 100;

	// how long this thread times out if it waits for other threads to all
	// finish moving
	public static final int ARM_MOVING_POLLING_RATE_TIME_MILLIS = 200;

	// one thread per arm
	private final Thread[] armThreads;

	// current goal lengths for every arm
	private final int[] lengths;

	// main watch thread
	private final Thread mainWatchThread;

	public SimpleRegulationImpl(ArmInterface[] arms, FSBServerInterface positionProvider) {
		super(arms, positionProvider);

		lengths = new int[arms.length];

		this.armThreads = new Thread[arms.length];
		for (int i = 0; i < this.armThreads.length; i++) {
			this.armThreads[i] = new Thread(new ArmThread(this, arms[i], i));
			this.armThreads[i].start();
		}

		mainWatchThread = new Thread(this);
		mainWatchThread.start();
	}

	// Main watch thread
	@Override
	public void run() {
		System.out.println("[MWT] all arms at starting position");

		while (Launcher.isRunning()) {
			// wait for all arms to be done moving
			waitForArmsToBeDone();
			System.out.println("[MWT] waiting for new position...");

			// get most recent position
			FSBPosition position = this.getMostRecentPosition();
			System.out.println("[MWT] new position: " + position.toString());

			// set lengths
			this.parsePosition(position);
			System.out.println("[MWT] parsed new position");

			// Wake up all Threads
			this.wakeArms();
			System.out.println("DEB notified");
		}
	}

	@Override
	public synchronized int getArmLength(int armId) {
		return this.lengths[armId];
	}

	public synchronized void setArmLength(int armId, int newLength) {
		this.lengths[armId] = newLength;
	}

	/**
	 * Checks arm thread for id and returns whether that arm is moving (Thread
	 * awake) or done moving (Thread sleeping)
	 * 
	 * @param arm
	 * @return
	 */
	public synchronized boolean isArmMoving(int armId) {
		// If the Thread is not waiting our arm must be moving. Simple
		// assumption we can make here. Still: Don't try this at home kids
		return this.armThreads[armId].getState() != Thread.State.WAITING;
	}

	/**
	 * Blocking method that waits for all arms to be @ Home
	 */
	private void waitForArmsToBeDone() {
		boolean allDoneMoving = false;

		while (!allDoneMoving) {
			// Assume all done
			allDoneMoving = true;

			// For every arm
			for (int i = 0; i < this.arms.length; i++) {
				// If one arm is still moving not all arms are done moving, duh
				if (this.isArmMoving(i)) {
					// Found one thats not done, reset to false
					allDoneMoving = false;
					break;
				}
			}

			// Arbitrary timeout to prevent resource wasting
			try {
				Thread.sleep(ARM_MOVING_POLLING_RATE_TIME_MILLIS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Wakes up all arms from Thread.State = WAITING
	 */
	private void wakeArms() {
		for (int i = 0; i < this.armThreads.length; i++) {
			if (!this.isArmMoving(i))
				synchronized (this.armThreads[i]) {
					this.armThreads[i].notify();
				}
		}
	}

	/**
	 * Receives most recent position. Blocking
	 * 
	 * @return
	 */
	private FSBPosition getMostRecentPosition() {
		FSBPosition position = null;
		try {
			position = positionProvider.getMostRecentPositionUpdate();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// exit if position is null
		if (position == null) {
			System.err.println("PositionProvider provided empty (null) position");
			Launcher.exit();
		}
		return position;
	}

	/**
	 * Parses position and stores it into lengths[]
	 * 
	 * @param position
	 */
	private void parsePosition(FSBPosition position) {
		this.setArmLength(0,
				(int) Math.round(((double) position.getLength1() / (double) FSBPosition.MAX) * (double) MAX_STEPS));
		this.setArmLength(1,
				(int) Math.round(((double) position.getLength2() / (double) FSBPosition.MAX) * (double) MAX_STEPS));
		this.setArmLength(2,
				(int) Math.round(((double) position.getLength3() / (double) FSBPosition.MAX) * (double) MAX_STEPS));
		this.setArmLength(3,
				(int) Math.round(((double) position.getLength4() / (double) FSBPosition.MAX) * (double) MAX_STEPS));
		this.setArmLength(4,
				(int) Math.round(((double) position.getLength5() / (double) FSBPosition.MAX) * (double) MAX_STEPS));
		this.setArmLength(5,
				(int) Math.round(((double) position.getLength6() / (double) FSBPosition.MAX) * (double) MAX_STEPS));
	}
}
