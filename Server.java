import java.io.*;
import java.net.*;
import java.util.Random;


public class Server {
    public static void main(String[] args) {
        // Ensure the correct number of arguments are provided
        if (args.length != 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }
        // Parse the port number from the command line
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide a valid integer.");
            return;
        }
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            System.out.println("Server is listening on port " + port);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);

                // Get request
                String receivedData = getRequest(requestPacket, serverSocket);

                String[] dataArr = receivedData.split(" ");
                String file = dataArr[1];

                // If file exists
                String response = String.format("ERR %s NOT_FOUND", file);
                File f = new File(file);
                if(f.exists()){
                    // Create new socket
                    DatagramSocket threadSocket = createSocket();

                    // Set response
                    response = String.format("OK %s %d %d", file, f.length(), threadSocket.getPort());

                    // Create thread with new socket and file
                    new Thread(() -> sendFile(f, threadSocket)).start();
                }

                // Send a response back to the client
                sendResponse(response, requestPacket, serverSocket);

                // Repeat
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static String getRequest(DatagramPacket requestPacket, DatagramSocket serverSocket){
        try{
            serverSocket.receive(requestPacket);
            String receivedData = new String(requestPacket.getData(), 0, requestPacket.getLength());
            System.out.println("Received file from client: " + receivedData);
            return receivedData;
        }
        catch(Exception e){
            return "ERR GET Request Error";
        }
    }

    private static void sendResponse(String response, DatagramPacket client, DatagramSocket serverSocket){
        try{
            // Send the response back to the client
            byte[] responseData = response.getBytes();
            InetAddress clientAddress = client.getAddress();
            int clientPort = client.getPort();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
            serverSocket.send(responsePacket);
            System.out.println("Response sent to client: " + response);
        }
        catch(Exception e){

        }

    }

    private static DatagramSocket createSocket(){
        try{
            Random r = new Random();
            int max=51000, min=50000;
            int port = r.nextInt(max - min + 1) + min;
            DatagramSocket socket = new DatagramSocket(port);
            return socket;

        }
        catch(Exception e){
            return null;   
        }    
    }

    private static void sendFile(File file, DatagramSocket socket){
        System.out.println("Thread was made");
    }
}