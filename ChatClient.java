import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ChatClient {

    private SSLSocket clientSocket;
    private BufferedReader incomingMessages;
    private PrintWriter outgoingMessages;

    private BufferedReader userInput;

    public ChatClient(String hostname, int port) {
        try {
            this.clientSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostname, port);
            clientSocket.startHandshake();
            this.userInput = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {}
    }

    public static void main(String[] args) {
        setupCertificate();

        ChatClient client = new ChatClient("localhost", 8888);

        client.openCommunicationChannels();
        client.createRecieverThread();

        while (true) {
            String message = client.getUserMessage();
            client.sendMessage(message);
        }

    }

    private static void setupCertificate() {
        System.setProperty("javax.net.ssl.trustStore", ".\\certs\\TrustStore.jts");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
    }

    private void createRecieverThread() {
        ChatClient client = this;
        Thread reciever = new Thread() {
            @Override
            public void run() {
                while (true) {
                    String message = recieveMessage();
                    client.printMessageToConsole(message);
                }
            }
        };
        reciever.start();
    }

    private static void clearConsoleLine() {
        System.out.print("\r");
        for (int i = 0; i < 30; i++) {
            System.out.print(" ");
        }
        System.out.print("\r");
    }

    private void printMessageToConsole(String message) {
        synchronized (this) {
            clearConsoleLine();
            System.out.println(message);
        }
    }

    private void openCommunicationChannels() {
        try {
            outgoingMessages = new PrintWriter(clientSocket.getOutputStream(), true);
            incomingMessages = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    private String getUserMessage() {
        try {
            return userInput.readLine();
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        return "";
    }

    private void sendMessage(String message) {
        this.outgoingMessages.println(message);
    }

    private String recieveMessage() {
        try {
            return this.incomingMessages.readLine();
        } catch (IOException e) {
            System.out.println("Connection terminated!");
            System.exit(1);
        }
        return "";
    }
}
