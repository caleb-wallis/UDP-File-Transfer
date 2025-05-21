import java.net.*;
import java.util.Scanner;
import java.io.File;  // Import the File class

public class Client {
    public static void main(String[] args) {
        // Ensure the correct number of arguments are provided
        if (args.length != 3) {
            System.out.println("Usage: java Client <hostname> <port> <filename>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String filename = args[2];
        File downloadFile = new File(filename);

        // Create a UDP socket
        try (DatagramSocket clientSocket = new DatagramSocket()) {

            System.out.println("Client is ready to send data.");

            // Request to download files from filename
            Scanner scanner = new Scanner(downloadFile);
           
            while (scanner.hasNextLine()) {
                // Get file to download
                String file = scanner.nextLine();
                String request = String.format("DOWNLOAD %s", file);

                // Send download request to the server
                sendRequest(request, clientSocket, port);

                // Receive the response from the server
                getResponse(clientSocket);
            }

            scanner.close();

        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    public static void sendRequest(String request, DatagramSocket clientSocket, int port){
        try{
            String hostname = "localhost";
            byte[] sendData = request.getBytes();
            InetAddress serverAddress = InetAddress.getByName(hostname);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
            clientSocket.send(sendPacket);

            System.out.println("Request sent to the server.");
        }
        catch(Exception e){System.out.println(e);}
    }

    public static void getResponse(DatagramSocket clientSocket){
        try{
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

            System.out.println("Server response: " + response);
        }
        catch(Exception e){System.out.println(e);}
    }
}