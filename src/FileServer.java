import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileServer {
    private int nPort;
    private ServerSocket serverSocket;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public FileServer(int nPort) {
        this.nPort = nPort;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(nPort);
            System.out.println("Server: Listening on port " + nPort + "...");

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

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;
        private FileServer server;
        private String handle;

        public ClientHandler(Socket socket, FileServer server) {
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
                    System.out.println("Received from " + handle + ": " + message);
                    processCommand(message);
                }
            } catch (IOException ex) {
                System.out.println("Client " + handle + " disconnected.");
            } finally {
                try {
                    socket.close();
                    server.removeClient(this);
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

        private void processCommand(String message) {
            String[] parts = message.split(" ");
            String command = parts[0];
            try {
                switch (command.toLowerCase()) {
                    case "/register":
                        if (parts.length != 2) {
                            sendMessage("Error: Incorrect command format for /register");
                            return;
                        }
                        for (ClientHandler client : clients) {
                            if (client.handle.equals(parts[1])) {
                                sendMessage("Error: Registration failed. Handle or alias already exists.");
                                return;
                            }
                        }
                        this.handle = parts[1];
                        sendMessage("Welcome " + this.handle + "!");
                        break;

                    case "/store":
                        if (parts.length != 3) {
                            sendMessage("Error: Incorrect command format for /store");
                            return;
                        }
                        String filename = parts[1];
                        long fileSize = Long.parseLong(parts[2]);
                        receiveFile(filename, fileSize);
                        break;

                    case "/dir":
                        if (parts.length != 1) {
                            sendMessage("Error: Incorrect command format for /dir");
                            return;
                        }
                        sendDirectory();
                        break;

                    case "/get":
                        if (parts.length != 2) {
                            sendMessage("Error: Incorrect command format for /get");
                            return;
                        }
                        sendFile(parts[1]);
                        break;

                    default:
                        server.broadcastMessage(message, this);
                        break;
                }
            } catch (IOException e) {
                System.out.println("Error processing command: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void receiveFile(String filename, long fileSize) throws IOException {
            File file = new File(filename);

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read = 0;
            long totalRead = 0;

            while (totalRead < fileSize) {
                read = this.input.read(buffer);
                totalRead += read;
                fos.write(buffer, 0, read);
            }
            fos.close();
            sendMessage("File " + filename + " received successfully");
        }

        private void sendDirectory() {
            File serverDirectory = new File(".");
            File[] files = serverDirectory.listFiles();

            StringBuilder listing = new StringBuilder("Directory Listing:\n");
            for (File file : files)
                listing.append(file.getName()).append("\n");

            sendMessage(listing.toString());
        }

        private void sendFile(String filename) {
            try {
                File file = new File(filename);

                if (!file.exists()) {
                    sendMessage("Error: File does not exist.");
                    return;
                }

                sendMessage("Sending " + file.getName() + " " + file.length());

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int read = 0;
                while ((read = fis.read(buffer)) > 0) {
                    this.output.write(buffer, 0, read);
                }
                fis.close();
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FileServer <port>");
            return;
        }

        int nPort = Integer.parseInt(args[0]);
        FileServer server = new FileServer(nPort);
        server.startServer();
    }
}
