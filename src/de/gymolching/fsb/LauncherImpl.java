package de.gymolching.fsb;

import com.pi4j.io.gpio.GpioFactory;

import java.util.logging.Logger;

/**
 * @author sschaeffner
 */
public class LauncherImpl implements LauncherInterface {
    private static final Logger log = Logger.getLogger(Launcher.class.getName());

    /**
     * Stops the program.
     */
    @Override
    public void exit() {
        log.fine("exiting...");

        //stop program
        System.out.println("waiting for shutdown...");
        Launcher.stop();
        try {
            GpioFactory.getInstance().shutdown();
        } catch (UnsupportedOperationException | NullPointerException e) {
            //do nothing
        }

        //wait for other threads to stop
        for (int i = 1; i < 20; i++) {
            System.out.print(".");
            if (i % 10 == 0) System.out.println();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("exit completed");
        System.exit(0);
    }
}
