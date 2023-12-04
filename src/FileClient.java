import java.io.*;
import java.net.*;

public class FileClient {

    private Socket socket;
    private BufferedReader consoleReader;
    private DataInputStream serverInput;
    private DataOutputStream serverOutput;
    private String handle;
    File fileToStore;

    private boolean isConnected = false;

    public FileClient() {
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        try {
            while (true) {
                System.out.print("> ");
                String input = consoleReader.readLine();

                parseCommand(input);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseCommand(String input) throws IOException {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command.toLowerCase()) {
            case "/?":
                showCommands();
                break;

            case "/join":
                if (!isConnected) {
                    System.out.println("You must connect to a server first using /join.");
                    return;
                }
                if (parts.length != 3) {
                    System.out.println("Input Syntax: /join <server_ip_add> <port>");
                    return;
                }
                connectToServer(parts[1], Integer.parseInt(parts[2]));
                break;

            case "/leave":
                leaveServer();
                break;

            case "/register":
                if (parts.length != 2) {
                    System.out.println("Input Syntax: /register <handle>");
                    return;
                }
                registerHandle(parts[1]);
                break;

            case "/store":
                if (parts.length != 2) {
                    System.out.println("Input Syntax: /store <filename>");
                    return;
                }
                storeFile(parts[1]);
                break;

            case "/dir":
                requestDirectory();
                break;

            case "/get":
                if (parts.length != 2) {
                    System.out.println("Input Syntax: /get <filename>");
                    return;
                }
                fetchFile(parts[1]);
                break;

            default:
                // sendMessage(input);
                System.out.println("Unknown command.");
                break;
        }
    }

    private void showCommands() {
        System.out.println("Available commands:");
        System.out.println("/join <server_ip_add> <port> - Connect to the server");
        System.out.println("/leave - Disconnect from the server");
        System.out.println("/register <handle> - Register a unique handle or alias");
        System.out.println("/store <filename> - Send file to server");
        System.out.println("/dir - Request directory file list from the server");
        System.out.println("/get <filename> - Fetch a file from the server");
    }

    private void connectToServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            serverInput = new DataInputStream(socket.getInputStream());
            serverOutput = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to server at " + ip + ":" + port);
            isConnected = true;
            // other handling server communication
        } catch (IOException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            isConnected = false;
        }
    }

    private void leaveServer() {
        // Implement server disconnection logic here
        // Close the socket and perform any necessary cleanup
        try {
            if (socket != null) {
                socket.close();
            }
            System.out.println("Disconnected from the server.");
            isConnected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerHandle(String newHandle) {
        if (handle != null) {
            System.out.println("Error: You are already registered with handle '" + handle + "'.");
        } else {
            sendMessage("/register " + newHandle);
            String response = receiveMessage();
            System.out.println(response);
            if (response.startsWith("Welcome")) {
                handle = newHandle;
            }
        }
    }

    private void storeFile(String filename) {
        try {
            File file = new File(filename);

            if (!file.exists()) {
                System.out.println("File does not exist.");
                return;
            }

            sendMessage("/store " + file.getName() + " " + file.length());

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = fis.read(buffer)) > 0) {
                serverOutput.write(buffer, 0, read);
            }
            fis.close();
            System.out.println("File sent successfully: " + file.getName());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void requestDirectory() {
        // Implement directory request logic here
        // Request the directory list from the server
        sendMessage("/dir");
        String response = receiveMessage();
        System.out.println(response);
    }

    private void fetchFile(String filename) {
        // Implement file fetching logic here
        // Request the specified file from the server
        sendMessage("/get " + filename);
        String response = receiveMessage();
        if (!response.startsWith("Sending")) {
            System.out.println("File does not exist.");
            return;
        }
        try {
            String[] parts = response.split(" ");
            long fileSize = Long.parseLong(parts[2]);
            receiveFile(filename, fileSize);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
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
            read = serverInput.read(buffer);
            totalRead += read;
            fos.write(buffer, 0, read);
        }
        fos.close();
        sendMessage("File " + filename + " received successfully");
    }

    private void sendMessage(String message) {
        try {
            serverOutput.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String receiveMessage() {
        try {
            return serverInput.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) {
        FileClient client = new FileClient();
        client.start();
    }
}
