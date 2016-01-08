package de.gymolching.fsb.regulation;

import de.gymolching.fsb.Launcher;
import de.gymolching.fsb.halApi.ArmInterface;

/**
 * One thread an arm.
 */
public class ArmThread implements Runnable {
	private final RegulationManagerInterface manager;
	private final ArmInterface arm;
	private final int armId;

	public ArmThread(RegulationManagerInterface manager, ArmInterface arm, int armId) {
		this.manager = manager;
		this.arm = arm;
		this.armId = armId;
	}

	@Override
	public void run() {
		// drive to starting position
		this.driveToHome();

		// Receive new
		while (Launcher.isRunning()) {
			System.out.println("[ARM" + armId + "] received new position");

			// Wait until woken up -> new position arrived. This WAITING
			// Thread.State also signals that this thread is done moving atm
			blockTillWakenUp();

			// Drive to new Position
			this.driveToPos(this.manager.getArmLength(armId));
			System.out.println("[ARM" + armId + "] at goal position");
		}
	}

	/**
	 * Drives to starting position
	 */
	private void driveToHome() {
		System.out.println("[ARM" + armId + "] moving to starting position");
		arm.moveToStartingPosition();
		System.out.println("[ARM" + armId + "] at starting position");
	}

	/**
	 * Blocks until current Thread is woken up
	 */
	private void blockTillWakenUp() {
		synchronized (Thread.currentThread()) {
			try {
				Thread.currentThread().wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Drives arm to goalPos. Blocking!
	 * 
	 * @param goalPos
	 */
	private void driveToPos(int goalPos) {
		int currentPos = arm.getPosition();

		arm.setSpeed(100);
		if (currentPos > goalPos) {
			arm.startBackward();
			while (currentPos > goalPos) {
				try {
					Thread.sleep(SimpleRegulationImpl.POLLING_RATE_TIME_MILLIS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				currentPos = arm.getPosition();
			}
		} else if (currentPos < goalPos) {
			arm.startForward();
			while (currentPos < goalPos) {
				try {
					Thread.sleep(SimpleRegulationImpl.POLLING_RATE_TIME_MILLIS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				currentPos = arm.getPosition();
			}
		}
		arm.stop();
	}
}
