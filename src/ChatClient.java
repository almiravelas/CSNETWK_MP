/*
 * LAB 04: PAIR WORK
 * Soriano, Erizha Marie Jayne (Server)
 * Velasquez, Almira Zabrina Alyson (Client)
 */
import java.io.*;
import java.net.*;

public class ChatClient {

    private Socket socket;
    private BufferedReader consoleReader;

    public ChatClient() {
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        while (true) {
            try {
                System.out.print("> ");
                String input = consoleReader.readLine();

                if(input.equals("/?")){
                    showCommands();
                }else{
                    parseCommand(input);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
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


    private void parseCommand(String input) throws IOException {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command.toLowerCase()) {
            case "/?": break;
            case "/join":
                if (parts.length != 3) {
                    System.out.println("Input Syntax: /join <server_ip_add> <port>");
                    return;
                }
                connectToServer(parts[1], Integer.parseInt(parts[2]));
                break;
            default:
                System.out.println("Unknown command.");
                break;
        }
    }

    private void connectToServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            new DataOutputStream(socket.getOutputStream());
            new DataInputStream(socket.getInputStream());
            System.out.println("Connected to server at " + ip + ":" + port);
            //other handling server communication
        } catch (IOException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
}

