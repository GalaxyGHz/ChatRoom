import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class ChatServer {

    private SSLServerSocket serverSocket;
    private ArrayList<SSLSocket> openSockets;

    public ChatServer(int port) {
        try {
            this.serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
        } catch (IOException e) {}
        this.openSockets = new ArrayList<>();
    }

    public static void main(String[] args) {
        setupCertificate();

        ChatServer server = new ChatServer(8888);
        
        while (true) {
            try {
                SSLSocket newSocket = (SSLSocket) server.serverSocket.accept();
                System.out.println("New socket connection: " + newSocket.toString());
                server.openSockets.add(newSocket);

                Thread thread = new Thread(new ConnectionHandler(server, newSocket));
                thread.start();
            } catch (IOException e) {
                System.out.println(e.toString());
                System.exit(1);
            }
        }
    }

    public ArrayList<SSLSocket> getSocketList() {
        return this.openSockets;
    }

    private static void setupCertificate() {
        System.setProperty("javax.net.ssl.keyStore", ".\\certs\\KeyStore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
    }

    static class ConnectionHandler implements Runnable {
        private ChatServer server;

        private SSLSocket socket;
        private BufferedReader in;
        private PrintWriter out;

        private ArrayList<PrintWriter> socketWriters;

        public ConnectionHandler(ChatServer server, SSLSocket socket) {
            this.server = server;
            this.socket = socket;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.out.println(e.toString());
                System.exit(1);
            }
            this.socketWriters = new ArrayList<>();
            this.getAllWriters();
        }

        private void getAllWriters() {
            for (SSLSocket sock : server.getSocketList()) {
                if (this.socket.equals(sock)) {
                    this.out = getSocketPrintWriter(sock);
                    this.socketWriters.add(this.out);
                    continue;
                }
                PrintWriter writer = getSocketPrintWriter(sock);
                this.socketWriters.add(writer);
            }
        }

        @Override
        public void run() {
            try {
                String message = null;
                while ((message = in.readLine()) != null) {
                    if (isInvalid(message)) continue;
                    System.out.println(message);
                    sendToAllOtherClients(message);
                }  
            } catch (IOException e) {
                System.out.println("Socket " + this.socket.toString() + " has lost connection!");
                this.server.openSockets.remove(this.socket);
            }
        }

        private void sendToAllOtherClients(String message) {
            for (PrintWriter writer : this.socketWriters) {
                if (writer.equals(this.out)) continue;
                writer.println(message);
            }
        }

        private PrintWriter getSocketPrintWriter(SSLSocket socket) {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println(e.toString());
                System.exit(1);
            }
            return writer;
        }

        private boolean isInvalid(String message) {
            if (message.equals("null") || message.equals("")) return true;
            return false;
        }
    }
}
