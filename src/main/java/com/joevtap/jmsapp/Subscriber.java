package com.joevtap.jmsapp;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Subscriber class that listens for JMS messages and processes them.
 */
public class Subscriber implements MessageListener {
    private Connection connection;
    private Session session;
    private MessageProducer replyProducer;

    /**
     * Starts the subscriber service to listen for messages
     */
    public void start() throws Exception {
        setupJMSConnection();

        // Keep application running to receive messages
        System.out.println("\n=== Subscriber Mode ===");
        System.out.println("Subscriber started and waiting for messages...");
        System.out.println("Press Ctrl+C to exit.");

        // Block main thread to keep subscriber running
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                shutdown();
            }
        }
    }

    /**
     * Sets up the JMS connection, session, and message consumer
     */
    private void setupJMSConnection() throws JMSException {
        // Create ConnectionFactory and JMS connection
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                JMSApplication.USERNAME,
                JMSApplication.PASSWORD,
                JMSApplication.BROKER_URL);

        connection = connectionFactory.createConnection();
        connection.setClientID("subscriberApp");
        connection.start();

        // Create session and destination
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(JMSApplication.QUEUE_NAME);

        // Create consumer and set this class as message listener
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);

        // Create a producer for sending responses
        replyProducer = session.createProducer(null);
    }

    /**
     * MessageListener implementation - called when a message is received
     */
    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                System.out.println("Invalid message received.");
                return;
            }

            TextMessage textMessage = (TextMessage) message;
            String eventType = textMessage.getStringProperty("eventType");
            String content = textMessage.getText();

            System.out.println("\nReceived event of type: " + eventType);

            // Check if JMSReplyTo is defined
            if (message.getJMSReplyTo() == null) {
                System.out.println("Warning: Message received without reply destination defined.");
                return;
            }

            // Process message based on event type
            switch (eventType) {
                case "ECHO":
                    processEcho(message, content);
                    break;
                case "CALCULATION":
                    processCalculation(message, content);
                    break;
                case "FILE_LIST":
                    processFileList(message);
                    break;
                case "FILE_CREATE":
                    processFileCreate(message, content);
                    break;
                case "FILE_VIEW":
                    processFileView(message, content);
                    break;
                case "FILE_EDIT":
                    processFileEdit(message, content);
                    break;
                case "FILE_SAVE":
                    processFileSave(message, content);
                    break;
                case "FILE_DELETE":
                    processFileDelete(message, content);
                    break;
                default:
                    System.out.println("Unrecognized event type.");
                    sendReply(message, "Error: Unknown event type.");
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    /**
     * Process an echo message
     */
    private void processEcho(Message message, String content) throws JMSException {
        System.out.println("Processing ECHO: " + content);
        sendReply(message, "Echo: " + content);
    }

    /**
     * Process a calculation message
     */
    private void processCalculation(Message message, String content) throws JMSException {
        System.out.println("Processing CALCULATION: " + content);
        String result = MathProcessor.calculate(content);
        sendReply(message, "Calculation result: " + result);
    }

    /**
     * Process a file listing request
     */
    private void processFileList(Message message) throws JMSException {
        System.out.println("Processing FILE_LIST request");

        try {
            File dir = new File(JMSApplication.FILE_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileList = "";
            File[] files = dir.listFiles();

            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    fileList += files[i].getName();
                    if (i < files.length - 1) {
                        fileList += "|";
                    }
                }
            } else {
                fileList = "No files found.";
            }

            sendReply(message, fileList);
        } catch (Exception e) {
            System.err.println("Error listing files: " + e.getMessage());
            sendReply(message, "Error: " + e.getMessage());
        }
    }

    /**
     * Process a file creation request
     */
    private void processFileCreate(Message message, String content) throws JMSException {
        System.out.println("Processing FILE_CREATE request");

        try {
            String[] parts = content.split("\\|", 2);
            if (parts.length < 2) {
                sendReply(message, "Error: Invalid format. Expected 'filename|content'.");
                return;
            }

            String filename = parts[0].trim();
            String fileContent = parts[1];

            // Ensure directory exists
            File dir = new File(JMSApplication.FILE_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create file
            File file = new File(JMSApplication.FILE_DIRECTORY + filename);
            if (file.exists()) {
                sendReply(message, "Error: File already exists.");
                return;
            }

            Files.writeString(file.toPath(), fileContent);
            sendReply(message, "File '" + filename + "' created successfully.");

        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            sendReply(message, "Error creating file: " + e.getMessage());
        }
    }

    /**
     * Process a file view request
     */
    private void processFileView(Message message, String filename) throws JMSException {
        System.out.println("Processing FILE_VIEW request for: " + filename);

        try {
            File file = new File(JMSApplication.FILE_DIRECTORY + filename);
            if (!file.exists()) {
                sendReply(message, "Error: File not found.");
                return;
            }

            String content = Files.readString(file.toPath());
            sendReply(message, content);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            sendReply(message, "Error reading file: " + e.getMessage());
        }
    }

    /**
     * Process a file edit request
     */
    private void processFileEdit(Message message, String filename) throws JMSException {
        System.out.println("Processing FILE_EDIT request for: " + filename);

        try {
            File file = new File(JMSApplication.FILE_DIRECTORY + filename);
            if (!file.exists()) {
                sendReply(message, "Error: File not found.");
                return;
            }

            String content = Files.readString(file.toPath());
            sendReply(message, content);

        } catch (IOException e) {
            System.err.println("Error reading file for edit: " + e.getMessage());
            sendReply(message, "Error reading file: " + e.getMessage());
        }
    }

    /**
     * Process a file save request
     */
    private void processFileSave(Message message, String content) throws JMSException {
        System.out.println("Processing FILE_SAVE request");

        try {
            String[] parts = content.split("\\|", 2);
            if (parts.length < 2) {
                sendReply(message, "Error: Invalid format. Expected 'filename|content'.");
                return;
            }

            String filename = parts[0].trim();
            String fileContent = parts[1];

            File file = new File(JMSApplication.FILE_DIRECTORY + filename);
            if (!file.exists()) {
                sendReply(message, "Error: File not found.");
                return;
            }

            Files.writeString(file.toPath(), fileContent);
            sendReply(message, "File '" + filename + "' saved successfully.");

        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
            sendReply(message, "Error saving file: " + e.getMessage());
        }
    }

    /**
     * Process a file delete request
     */
    private void processFileDelete(Message message, String filename) throws JMSException {
        System.out.println("Processing FILE_DELETE request for: " + filename);

        try {
            File file = new File(JMSApplication.FILE_DIRECTORY + filename);
            if (!file.exists()) {
                sendReply(message, "Error: File not found.");
                return;
            }

            if (file.delete()) {
                sendReply(message, "File '" + filename + "' deleted successfully.");
            } else {
                sendReply(message, "Error: Could not delete file.");
            }
        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            sendReply(message, "Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Sends a reply to a message
     */
    private void sendReply(Message request, String reply) throws JMSException {
        Destination replyDestination = request.getJMSReplyTo();
        if (replyDestination != null) {
            try {
                TextMessage responseMessage = session.createTextMessage(reply);

                // Set correlation ID to link with original request
                String correlationId = request.getJMSCorrelationID();
                if (correlationId != null) {
                    responseMessage.setJMSCorrelationID(correlationId);
                }

                replyProducer.send(replyDestination, responseMessage);
                System.out.println("Reply sent: " + reply);
            } catch (JMSException e) {
                System.err.println("Error sending reply: " + e.getMessage());
            }
        } else {
            System.out.println("No reply destination defined for this message.");
        }
    }

    /**
     * Cleanly shuts down JMS resources
     */
    private void shutdown() {
        System.out.println("Shutting down Subscriber...");
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