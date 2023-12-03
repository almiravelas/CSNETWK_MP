import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private int nPort;
    private ServerSocket serverSocket;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public ChatServer(int nPort){
        this.nPort = nPort;
    }

    public void startServer() {

        try{
            serverSocket = new ServerSocket(nPort);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void broadcastMessage(String message, ClientHandler excludeClient) {
        for (ClientHandler aClient : clients) {
            if (aClient != excludeClient) {
                aClient.sendMessage(message);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;
        private ChatServer server;

        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.out.println("Handler exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = input.readUTF();
                    System.out.println("Received from " + socket.getRemoteSocketAddress() + ": " + message);
                    server.broadcastMessage(message, this);
                }
            } catch (IOException ex) {
                System.out.println("Client " + socket.getRemoteSocketAddress() + " disconnected.");
            } finally {
                try {
                    socket.close();
                    clients.remove(this);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            try {
                output.writeUTF(message);
            } catch (IOException ex) {
                System.out.println("Error sending message to client: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: java ChatServer <port>");
                return;
            }

            int nPort = Integer.parseInt(args[0]);
            System.out.println("Server: Listening on port " + nPort + "...");

        ChatServer server = new ChatServer(nPort);
        server.startServer();
    }
}
