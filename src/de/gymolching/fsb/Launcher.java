package de.gymolching.fsb;

import com.pi4j.io.gpio.GpioFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.*;

/**
 * This checks for pi4j and launches the program.
 * @author sschaeffner
 */
public class Launcher {

    //Logging
    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    //program name
    private static final String NAME = "FSB";
    //program version
    private static final String VERSION = "0.1";

    //whether the program should currently be running
    private static boolean running;

    //whether the program is currently exiting
    private static boolean exiting;

    //Launcher Impl
    private static LauncherInterface launcher;

    public static void main(String[] args) {
        //setup logging
        setupLogging();

        launcher = new LauncherImpl();

        log.fine("checking for pi4j...");
        if (checkForPi4J()) {
            log.finer("pi4j in classpath");
            running = true;
            exiting = false;


            //create thread for program
            Thread t = new Thread(() -> {
                MainLoopHandler.getInstance().mainLoop();
            }, "program");
            t.start();
            log.fine("MainLoopHandler started");

            //start MiniConsole (blocking until user inputs "exit")
            new MiniConsole().start();

            exit();
        }
    }

    /**
     * Sets up the main logger.
     */
    private static void setupLogging() {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        // suppress the logging output to the console
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        logger.setLevel(Level.ALL);

        try {
            FileHandler fileTxt = new FileHandler("log.txt");
            // create a TXT formatter
            SimpleFormatter formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);

        logger.addHandler(consoleHandler);

        logger.fine("logger fine");
        log.finer("log finer");
    }

    /**
     * Exits the program.
     */
    public static void exit() {
        if (!exiting) {
            log.finest("exiting...");
            log.finest(".");
            log.finest(".");
            log.finest(".");
            exiting = true;

            //stop program
            System.out.println("waiting for shutdown...");
            stop();
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
            System.out.println("shutdown completed");
            System.exit(0);
        }
    }

    /**
     * Checks whether Pi4J is in the classpath.
     * @return  whether Pi4J is in the classpath
     */
    public static boolean checkForPi4J() {
        try {
            Class.forName("com.pi4j.io.gpio.GpioFactory");
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("Pi4J could not be found in /opt/pi4j or in classpath");
            System.err.println("please install Pi4J in the default location");
            return false;
        }
    }

    /**
     * Returns whether the program should currently be running.
     * @return whether the program should currently be running.
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * Stops the program gracefully.
     */
    public static void stop() {
        running = false;
    }

    /**
     * A simple console.
     */
    public static class MiniConsole {
        private Scanner scanner;
        private static final String WELCOME_MESSAGE = "Welcome to " + NAME + " v" + VERSION + "!" + System.lineSeparator() + "Enter help for a list of commands.";
        private static final String EXIT_COMMAND = "exit";
        private static final String CONSOLE_PROMPT = "$ ";

        public MiniConsole() {
            scanner = new Scanner(System.in);
        }

        /**
         * Starts the console.
         * This is blocking until the user inputs "exit".
         */
        public void start() {
            System.out.println(WELCOME_MESSAGE);

            System.out.print(CONSOLE_PROMPT);
            String input;
            while (!(input = scanner.nextLine()).equalsIgnoreCase(EXIT_COMMAND)) {
                input = input.toLowerCase();

                switch (input) {
                    case "help":
                        System.out.println(NAME + " v" + VERSION);
                        System.out.println("exit    exits the program");
                        System.out.println("help    prints this help");
                        break;
                    default:
                        System.err.println("Unknown command. Enter help for a list of commands.");
                        break;
                }

                System.out.print(CONSOLE_PROMPT);
            }
        }
    }
}
