import java.io.*;
import java.net.*;

public class FileClient{

    private Socket socket;
    private BufferedReader consoleReader;
    private DataInputStream serverInput;
    private DataOutputStream serverOutput;
    private String handle;
    File fileToStore; 

    private boolean isConnected = false;

    private String clientDirectory;
    private String serverDirectory;

    public FileClient(String clientDirectory, String serverDirectory) {
        this.clientDirectory = clientDirectory;
        this.serverDirectory = serverDirectory;
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        try {
            while (true) {
                System.out.print("> ");
                String input = consoleReader.readLine();

                if (input.equals("/?")) {
                    showCommands();
                } else {
                    parseCommand(input);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showCommands() {
        System.out.println("Available commands:");
        System.out.println("/join <server_ip_add> <port> /t - Connect to the server");
        System.out.println("/leave /t - Disconnect from the server");
        System.out.println("/register <handle> /t - Register a unique handle or alias");
        System.out.println("/store <filename> /t - Send file to server");
        System.out.println("/dir /t - Request directory file list from the server");
        System.out.println("/get <filename> /t - Fetch a file from the server");
    }

    private void parseCommand(String input) throws IOException {
        String[] parts = input.split(" ");
        String command = parts[0];

        if (!isConnected && !input.startsWith("/join")) {
            System.out.println("You must connect to a server first using /join.");
            return;
        }

        switch (command.toLowerCase()) {
            case "/?":
                showCommands();
                break;

            case "/join":
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
                System.out.println("Error: Command not found.");
                break;
        }
    }

    private void connectToServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            serverInput = new DataInputStream(socket.getInputStream());
            serverOutput = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to server at " + ip + ":" + port);
            System.out.println("Connection to the File Exchange Server is successful!");
            isConnected = true;
        } catch (IOException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
            isConnected = false;
        }
    }

    private void leaveServer() {
        try {
            if (socket != null) {
                socket.close();
            }
            System.out.println("Connection closed. Thank you!");
            isConnected = false;
        } catch (IOException e) {
            System.out.println("Error: Disconnection failed. Please connect to the server first.");
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
        try{
            File file = new File(clientDirectory, filename);

            if(!file.exists()){
                System.out.println("File does not exist.");
                return;
            }

            sendMessage("/store "+ file.getName()+" "+ file.length());
            sendFile(file);
            // System.out.println(handle + "<" + new Timestamp(System.currentTimeMillis()) + ">: Uploaded " + filename);
        }catch (IOException e){
            System.out.println("Error: File not found "+ e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendFile(File file) throws IOException{
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int read = 0;
        while((read = fis.read(buffer)) > 0){
            serverOutput.write(buffer, 0, read);
        }
        fis.close();
        System.out.println("File sent successfully: " + file.getName());
    }

    private void requestDirectory() {
        sendMessage("/dir");
        String response = receiveMessage();
        System.out.println(response);
    }

    public void fetchFile(String filename) {
        sendMessage("/get " + filename);
        String response = receiveMessage();
        if (!response.startsWith("Sending")) {
            System.out.println("File does not exist on the server.");
            return;
        }
        try {
            String[] parts = response.split(" ");
            long fileSize = Long.parseLong(parts[2]);
            receiveFile(serverDirectory, filename, fileSize);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private void receiveFile(String clientDirectory, String filename, long fileSize) throws IOException {
        File file = new File(clientDirectory, filename);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs(); // Create parent directories if they do not exist
        }
       
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            long totalRead = 0;
            while (totalRead < fileSize) {
                read = serverInput.read(buffer);
                if (read == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                totalRead += read;
                fos.write(buffer, 0, read);
            }
        }
        // Wait for the completion signal
        String completionSignal = receiveMessage();
        if ("File transfer complete.".equals(completionSignal)) {
            System.out.println("File received from Server: " + filename);
        } else {
            System.out.println("Error in file transfer.");
        }
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
        String clientDir = "C:\\Users\\63935\\OneDrive\\Desktop\\Test_netwk\\CLIENT";
        String serverDir = "C:\\Users\\63935\\OneDrive\\Desktop\\Test_netwk\\SERVER";
        FileClient client = new FileClient(clientDir, serverDir);

        client.start();
    }
}
