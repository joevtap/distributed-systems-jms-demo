package com.joevtap.jmsapp;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.util.Scanner;
import java.util.UUID;

/**
 * Publisher class responsible for sending messages to the JMS broker
 * and handling responses from the Subscriber.
 */
public class Publisher {
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private Destination destination;
    private Destination replyDestination;
    private String correlationId;

    /**
     * Starts the publisher service and handles user input
     */
    public void start(Scanner scanner) throws Exception {
        setupJMSConnection();

        boolean continueRunning = true;

        while (continueRunning) {
            JMSApplication.clearConsole();
            displayMainMenu();

            int option = scanner.nextInt();
            scanner.nextLine(); // Consume line break

            if (option == 0) {
                continueRunning = false;
                continue;
            }

            processMenuOption(option, scanner);

            System.out.println("\nPress ENTER to continue...");
            scanner.nextLine();
        }

        // Clean up resources
        shutdown();
    }

    /**
     * Sets up the JMS connection, session, and message producer
     */
    private void setupJMSConnection() throws JMSException {
        // Create ConnectionFactory and JMS connection
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                JMSApplication.USERNAME,
                JMSApplication.PASSWORD,
                JMSApplication.BROKER_URL);

        connection = connectionFactory.createConnection();
        connection.start();

        // Create session and destinations
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = session.createQueue(JMSApplication.QUEUE_NAME);
        replyDestination = session.createQueue(JMSApplication.REPLY_QUEUE_NAME);

        // Create producer
        producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    }

    /**
     * Displays the main menu options for the publisher
     */
    private void displayMainMenu() {
        System.out.println("\n=== Publisher Mode ===");
        System.out.println("Choose event type:");
        System.out.println("1 - Send message (Echo)");
        System.out.println("2 - Send math expression (Calculation)");
        System.out.println("3 - File operations");
        System.out.println("0 - Exit");
        System.out.print("Option: ");
    }

    /**
     * Processes the selected menu option
     */
    private void processMenuOption(int option, Scanner scanner) throws JMSException {
        switch (option) {
            case 1:
                sendEchoMessage(scanner);
                break;
            case 2:
                sendCalculationMessage(scanner);
                break;
            case 3:
                new FileOperations(session, producer, replyDestination).handleFileOperations(scanner);
                break;
            default:
                System.out.println("Invalid option. Try again.");
                break;
        }
    }

    /**
     * Handles sending an echo message and processing the response
     */
    private void sendEchoMessage(Scanner scanner) throws JMSException {
        TextMessage message = session.createTextMessage();
        message.setStringProperty("eventType", "ECHO");

        System.out.println("Enter message:");
        message.setText(scanner.nextLine());

        sendAndReceive(message);
    }

    /**
     * Handles sending a calculation message and processing the response
     */
    private void sendCalculationMessage(Scanner scanner) throws JMSException {
        TextMessage message = session.createTextMessage();
        message.setStringProperty("eventType", "CALCULATION");

        System.out.println("Enter math expression (e.g.: 5+3):");
        message.setText(scanner.nextLine());

        sendAndReceive(message);
    }

    /**
     * Common method to send a message and wait for a response
     */
    private void sendAndReceive(Message message) throws JMSException {
        correlationId = UUID.randomUUID().toString();
        message.setJMSCorrelationID(correlationId);
        message.setJMSReplyTo(replyDestination);

        producer.send(message);

        if (message instanceof TextMessage) {
            System.out.println("Message sent: " + ((TextMessage) message).getText());
        }

        System.out.println("Waiting for Subscriber response (max 30 seconds)...");

        // Create a selector to receive only responses with the specific correlation ID
        String selector = "JMSCorrelationID='" + correlationId + "'";
        MessageConsumer responseConsumer = session.createConsumer(replyDestination, selector);

        Message response = responseConsumer.receive(30000); // 30 seconds timeout

        if (response instanceof TextMessage) {
            TextMessage textResponse = (TextMessage) response;
            System.out.println("Response received: " + textResponse.getText());
        } else {
            System.out.println("Timeout. Subscriber may be offline or busy.");
        }

        responseConsumer.close();
    }

    /**
     * Cleanly shuts down JMS resources
     */
    private void shutdown() {
        System.out.println("Terminating Publisher mode...");
        try {
            if (session != null)
                session.close();
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            System.err.println("Error while shutting down: " + e.getMessage());
        }
    }
}