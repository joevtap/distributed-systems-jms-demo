package com.joevtap.jmsapp;

import java.util.Scanner;

public class JMSApplication {
    // Load configuration from environment variables
    private static final Config config = Config.getInstance();
    
    // Properties accessed through Config
    public static final String BROKER_URL = config.getBrokerUrl();
    public static final String QUEUE_NAME = config.getQueueName();
    public static final String REPLY_QUEUE_NAME = config.getReplyQueueName();
    public static final String USERNAME = config.getUsername();
    public static final String PASSWORD = config.getPassword();
    public static final String FILE_DIRECTORY = config.getFileDirectory();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        clearConsole();
        System.out.println("=== JMS Application ===");
        System.out.println("Select application mode:");
        System.out.println("1 - Publisher");
        System.out.println("2 - Subscriber");
        System.out.print("Option: ");

        int mode = scanner.nextInt();
        scanner.nextLine(); // Consume line break

        try {
            if (mode == 1) {
                Publisher publisher = new Publisher();
                publisher.start(scanner);
            } else if (mode == 2) {
                Subscriber subscriber = new Subscriber();
                subscriber.start();
            } else {
                System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * Utility method to clear the console screen
     */
    public static void clearConsole() {
        try {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                // For Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // For Unix/Linux/MacOS
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing console fails, just print some newlines
            System.out.println("\n\n\n\n\n");
        }
    }
}