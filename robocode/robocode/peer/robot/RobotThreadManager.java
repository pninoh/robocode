/*******************************************************************************
 * Copyright (c) 2001, 2007 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/cpl-v10.html
 *
 * Contributors:
 *     Mathew A. Nelson
 *     - Initial API and implementation
 *     Flemming N. Larsen
 *     - Updated to use methods from the Logger, which replaces logger methods
 *       that have been (re)moved from the robocode.util.Utils class
 *     - Moved the stopThread() method from the RobocodeDeprecated class into
 *       this class
 *     - Code cleanup
 *******************************************************************************/
package robocode.peer.robot;


import static robocode.io.Logger.log;
import robocode.peer.RobotPeer;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class RobotThreadManager {
	private RobotPeer robotPeer;
	private Thread runThread;
	private ThreadGroup runThreadGroup;

	public RobotThreadManager(RobotPeer robotPeer) {
		this.robotPeer = robotPeer;
		runThreadGroup = new ThreadGroup(robotPeer.getName());
		runThreadGroup.setMaxPriority(Thread.NORM_PRIORITY);
	}

	public void forceStop() {
		if (runThread != null && runThread.isAlive()) {
			runThread.setPriority(Thread.MIN_PRIORITY);
			runThread.interrupt();
			try {
				runThread.join(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			robotPeer.setRunning(false);
			robotPeer.getRobotStatistics().setInactive();
			if (runThread.isAlive()) {
				stopThread(runThread);
			}
			try {
				runThread.join(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();				
			}
			if (runThread.isAlive()) {
				log("Warning!  Unable to stop thread: " + runThread.getName());
			} else {
				robotPeer.out.println("SYSTEM: This robot has been stopped.  No score will be generated.");
				log(robotPeer.getName() + " has been stopped.  No score will be generated.");
			}
		}

		Thread[] threads = new Thread[10];
		int numThreads = runThreadGroup.enumerate(threads);

		if (numThreads == 1 && threads[0] == runThread) {
			return;
		}

		if (numThreads != 0) {
			robotPeer.getRobotStatistics().setInactive();
			robotPeer.out.println(
					"SYSTEM:  You still have " + numThreads + " running threads.  No score will be generated.");
		}
		for (Thread thread : threads) {
			if (thread != null) {
				thread.setPriority(Thread.MIN_PRIORITY);
				stopThread(thread);
			}
		}
		for (Thread thread : threads) {
			if (thread != null) {
				try {
					thread.join(1000);
				} catch (InterruptedException e) {
					robotPeer.out.println("SYSTEM:  Thread: " + thread.getName() + " join interrupted.");
					log("Thread: " + thread.getName() + " join interrupted.");
				}
				if (thread.isAlive()) {
					log("Warning! Unable to stop thread: " + thread.getName());
				} else {
					robotPeer.out.println("SYSTEM:  Thread: " + thread.getName() + " has been stopped.");
					log("Thread: " + thread.getName() + " has been stopped.");
				}
			}
		}
	}

	public ThreadGroup getThreadGroup() {
		return runThreadGroup;
	}

	public void cleanup() {
		try {
			runThreadGroup.destroy();
		} catch (Exception e) {
			log("Warning, could not destroy " + runThreadGroup.getName(), e);
		}
	}

	public void start() {
		try {
			runThread = new Thread(runThreadGroup, robotPeer, robotPeer.getName());
			runThread.setDaemon(true);
			runThread.setPriority(Thread.NORM_PRIORITY);
			runThread.start();
		} catch (Exception e) {
			log("Exception starting thread: " + e);
		}
	}

	public void waitForStop() {
		if (runThread == null) {
			return;
		}
		runThread.interrupt();
		for (int j = 0; j < 20 && runThreadGroup.activeCount() > 0; j++) {
			if (j == 10) {
				log("Waiting for robot: " + robotPeer.getName() + " to stop");
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
		if (runThread.isAlive()) {
			log(robotPeer.getName() + " is not stopping.  Forcing a stop.");
			forceStop();
		}
		if (runThreadGroup.activeCount() > 0) {
			if (!System.getProperty("NOSECURITY", "false").equals("true")) {
				log("Robot " + robotPeer.getName() + " has threads still running.  Forcing a stop.");
				forceStop();
			} else {
				log("Robot " + robotPeer.getName()
						+ " has threads still running.  Not stopping them because security is off.");
			}
		}
	}

	/**
	 * Gets the runThread.
	 *
	 * @return Returns a Thread
	 */
	public Thread getRunThread() {
		return runThread;
	}

	@SuppressWarnings("deprecation")
	private void stopThread(Thread t) {
		synchronized (runThread) {
			t.stop();
		}
	}
}
