import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        // Ensure the correct number of arguments are provided
        if (args.length != 2) {
            System.out.println("Usage: java Client <hostname> <port>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        // Create a UDP socket
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            System.out.println("Client is ready to send data.");
            Scanner scanner = new Scanner(System.in);
            // Input a date from the user
            System.out.print("Enter a date (YYYY-MM-DD): ");
            String date = scanner.nextLine();


            
            // Send the date to the server
            byte[] sendData = date.getBytes();
            InetAddress serverAddress = InetAddress.getByName(hostname);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                    serverAddress, port);
            clientSocket.send(sendPacket);
            System.out.println("Date sent to the server.");



            // Receive the response (weekday) from the server
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, 
                receivePacket.getLength());
            System.out.println("Server response: " + response);

        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}