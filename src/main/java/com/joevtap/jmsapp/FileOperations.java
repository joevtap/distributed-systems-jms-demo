package com.joevtap.jmsapp;

import javax.jms.*;
import java.util.Scanner;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for handling file operations via JMS.
 */
public class FileOperations {
    private final Session session;
    private final MessageProducer producer;
    private final Destination replyDestination;

    /**
     * Constructor to initialize with JMS resources
     */
    public FileOperations(Session session, MessageProducer producer, Destination replyDestination) {
        this.session = session;
        this.producer = producer;
        this.replyDestination = replyDestination;
    }

    /**
     * Displays the file operations menu and handles user input
     */
    public void handleFileOperations(Scanner scanner) throws JMSException {
        JMSApplication.clearConsole();
        System.out.println("\n=== File Operations ===");
        System.out.println("1 - List files");
        System.out.println("2 - Create new file");
        System.out.println("3 - View file content");
        System.out.println("4 - Edit file");
        System.out.println("5 - Delete file");
        System.out.println("0 - Back to main menu");
        System.out.print("Option: ");

        int fileOption = scanner.nextInt();
        scanner.nextLine(); // Consume line break

        if (fileOption == 0) {
            return;
        }

        processFileOperation(fileOption, scanner);
    }

    /**
     * Gets a filename from the user, with option to select from numbered list or
     * enter manually
     */
    private String getFilenameFromUser(Scanner scanner, String promptMessage) throws JMSException {
        // First, get the list of available files
        List<String> availableFiles = listAvailableFiles();

        if (availableFiles.isEmpty()) {
            System.out.println("No files available. Please enter a new filename:");
            return scanner.nextLine();
        }

        // Display the list of files with numbers
        System.out.println("\n" + promptMessage);
        System.out.println("Available files:");

        for (int i = 0; i < availableFiles.size(); i++) {
            System.out.println((i + 1) + ". " + availableFiles.get(i));
        }

        System.out.println("Enter a number to select a file, or '/' to enter filename manually:");
        String selection = scanner.nextLine().trim();

        if (selection.equals("/")) {
            System.out.println("Enter filename:");
            return scanner.nextLine();
        } else {
            try {
                int index = Integer.parseInt(selection) - 1;
                if (index >= 0 && index < availableFiles.size()) {
                    return availableFiles.get(index);
                } else {
                    System.out.println("Invalid selection. Please enter a filename:");
                    return scanner.nextLine();
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a filename:");
                return scanner.nextLine();
            }
        }
    }

    /**
     * Gets a list of available files from the server via JMS
     */
    private List<String> listAvailableFiles() throws JMSException {
        List<String> fileList = new ArrayList<>();

        // Create a message to request the file list
        TextMessage message = session.createTextMessage();
        message.setStringProperty("eventType", "FILE_LIST");
        message.setText("LIST");
        message.setJMSReplyTo(replyDestination);

        // Generate correlation ID
        String correlationId = UUID.randomUUID().toString();
        message.setJMSCorrelationID(correlationId);

        // Send the request
        producer.send(message);
        System.out.println("Requesting file list...");

        // Receive the response
        String selector = "JMSCorrelationID='" + correlationId + "'";
        MessageConsumer responseConsumer = session.createConsumer(replyDestination, selector);

        Message response = responseConsumer.receive(10000); // 10 seconds timeout

        if (response instanceof TextMessage) {
            TextMessage textResponse = (TextMessage) response;
            String responseText = textResponse.getText();

            if (!responseText.equals("No files found.")) {
                String[] files = responseText.split("\\|");
                for (String file : files) {
                    fileList.add(file);
                }
            }
        }

        responseConsumer.close();
        return fileList;
    }

    /**
     * Processes the selected file operation
     */
    private void processFileOperation(int fileOption, Scanner scanner) throws JMSException {
        TextMessage message = session.createTextMessage();
        message.setJMSReplyTo(replyDestination);

        // Generate correlation ID
        String correlationId = UUID.randomUUID().toString();
        message.setJMSCorrelationID(correlationId);

        // File name variable available throughout the method
        String filename = "";

        switch (fileOption) {
            case 1: // List files
                message.setStringProperty("eventType", "FILE_LIST");
                message.setText("LIST");
                break;

            case 2: // Create new file
                System.out.println("Enter filename to create:");
                String newFilename = scanner.nextLine();
                System.out.println("Enter file content:");
                String newContent = scanner.nextLine();

                message.setStringProperty("eventType", "FILE_CREATE");
                message.setText(newFilename + "|" + newContent);
                break;

            case 3: // View file content
                filename = getFilenameFromUser(scanner, "Select a file to view:");

                message.setStringProperty("eventType", "FILE_VIEW");
                message.setText(filename);
                break;

            case 4: // Edit file
                filename = getFilenameFromUser(scanner, "Select a file to edit:");

                message.setStringProperty("eventType", "FILE_EDIT");
                message.setText(filename);
                break;

            case 5: // Delete file
                filename = getFilenameFromUser(scanner, "Select a file to delete:");

                message.setStringProperty("eventType", "FILE_DELETE");
                message.setText(filename);
                break;

            default:
                System.out.println("Invalid option.");
                return;
        }

        // Send request and handle response
        sendFileRequestAndHandleResponse(message, fileOption, filename, scanner);
    }

    /**
     * Sends a file operation request and processes the response
     */
    private void sendFileRequestAndHandleResponse(Message message, int fileOption, String filename, Scanner scanner)
            throws JMSException {
        producer.send(message);
        System.out.println("Request sent with operation: " + message.getStringProperty("eventType"));
        System.out.println("Waiting for Subscriber response (max 30 seconds)...");

        // Handle the response
        String selector = "JMSCorrelationID='" + message.getJMSCorrelationID() + "'";
        MessageConsumer responseConsumer = session.createConsumer(replyDestination, selector);

        Message response = responseConsumer.receive(30000); // 30 seconds timeout

        if (response instanceof TextMessage) {
            TextMessage textResponse = (TextMessage) response;
            String responseText = textResponse.getText();

            // Special handling for edit operation
            if (fileOption == 4 && !responseText.startsWith("Error:")) {
                handleFileEdit(responseText, filename, scanner);
            } else {
                displayFileOperationResponse(fileOption, responseText);
            }
        } else {
            System.out.println("Timeout. Subscriber may be offline or busy.");
        }

        responseConsumer.close();
    }

    /**
     * Handles the file edit operation after receiving the current file content
     */
    private void handleFileEdit(String currentContent, String filename, Scanner scanner) throws JMSException {
        System.out.println("\nCurrent content of the file:");
        System.out.println(currentContent);
        System.out.println("\nEnter new content (or press Enter to keep existing):");
        String editedContent = scanner.nextLine();

        if (editedContent.isEmpty()) {
            System.out.println("No changes made to the file.");
            return;
        }

        // Send another message to save the edited content
        TextMessage saveMessage = session.createTextMessage();
        saveMessage.setStringProperty("eventType", "FILE_SAVE");
        saveMessage.setText(filename + "|" + editedContent);
        saveMessage.setJMSReplyTo(replyDestination);

        String saveCorrelationId = UUID.randomUUID().toString();
        saveMessage.setJMSCorrelationID(saveCorrelationId);

        producer.send(saveMessage);
        System.out.println("Sending updated content...");

        // Wait for save confirmation
        String saveSelector = "JMSCorrelationID='" + saveCorrelationId + "'";
        MessageConsumer saveResponseConsumer = session.createConsumer(replyDestination, saveSelector);
        Message saveResponse = saveResponseConsumer.receive(30000);

        if (saveResponse instanceof TextMessage) {
            System.out.println("Response: " + ((TextMessage) saveResponse).getText());
        } else {
            System.out.println("No response received for file save operation.");
        }

        saveResponseConsumer.close();
    }

    /**
     * Displays the response from a file operation in an appropriate format
     */
    private void displayFileOperationResponse(int fileOption, String responseText) {
        System.out.println("Response received:");

        if (fileOption == 1) { // List files
            System.out.println("Available files:");
            if (responseText.isEmpty() || responseText.equals("No files found.")) {
                System.out.println("No files found.");
            } else {
                String[] files = responseText.split("\\|");
                for (String file : files) {
                    System.out.println("- " + file);
                }
            }
        } else if (fileOption == 3) { // View file
            System.out.println("File content:");
            System.out.println(responseText);
        } else {
            System.out.println(responseText);
        }
    }
}