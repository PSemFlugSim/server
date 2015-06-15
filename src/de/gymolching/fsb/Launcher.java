package de.gymolching.fsb;

import com.pi4j.io.gpio.GpioFactory;

import java.util.Scanner;

/**
 * This checks for pi4j and launches the program.
 * @author sschaeffner
 */
public class Launcher {

    //program name
    private static final String NAME = "FSB";
    //program version
    private static final String VERSION = "0.0";

    //whether the program should currently be running
    private static boolean running;

    public static void main(String[] args) {
        if (checkForPi4J()) {
            running = true;

            //create thread for program
            Thread t = new Thread(() -> {

                //TODO launch program here

            }, "program");
            t.start();

            //start MiniConsole (blocking until user inputs "exit")
            new MiniConsole().start();

            //stop program
            System.out.println("waiting for shutdown...");
            stop();
            try {
                GpioFactory.getInstance().shutdown();
            } catch (UnsupportedOperationException | NullPointerException e) {
                //do nothing
            }

            //wait for other threads to stop
            for (int i = 0; i < 20; i++) {
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
            System.err.println("Pi4J could not be found in /opt/pi4j");
            System.err.println("please install Pi4J in the default location");
            System.err.println();
            System.err.println();
            e.printStackTrace();
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
