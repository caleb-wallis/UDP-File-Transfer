import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Base64;
import java.util.Arrays;

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
            System.out.println("Server is listening on port " + port + ": LOCAL PORT : " + serverSocket.getLocalPort());

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket client = new DatagramPacket(buffer, buffer.length);

                // Get request
                String receivedData = getRequest(client, serverSocket);

                String[] dataArr = receivedData.split(" ");
                String file = dataArr[1];

                // If file exists
                String response = String.format("ERR %s NOT_FOUND", file);
                File f = new File(file);
                if(f.exists()){
                    // Create new socket
                    DatagramSocket threadSocket = createSocket();

                    // Set response
                    response = String.format("OK %s SIZE %d PORT %d", file, f.length(), threadSocket.getLocalPort());

                    // Create thread with new socket and file
                    new Thread(() -> sendFile(file, client, threadSocket)).start();
                }

                // Send a response back to the client
                sendResponse(response, client, serverSocket);

                // Repeat
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static String getRequest(DatagramPacket requestPacket, DatagramSocket serverSocket){
        try{
            serverSocket.receive(requestPacket);
            String request = new String(requestPacket.getData(), 0, requestPacket.getLength());
            System.out.println("Request from client: " + request);
            return request;
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
            System.out.println("Created Socket on Port: " + socket.getLocalPort());
            return socket;
        }
        catch(Exception e){
            System.out.println(e);
            return null;   
        }    
    }

    private static void sendFile(String file, DatagramPacket client, DatagramSocket socket){
        System.out.println("Thread was made");
        boolean get = true;

        while(get){
            // Get download request
            String request = getRequest(client, socket);
            String[] dataArr = request.split(" ");

            // Check 3rd value for CLOSE or GET
            String operation = dataArr[2];

            // Perform Operation
            String response = "";

            if(operation.equals("GET")){
                int start = Integer.parseInt(dataArr[4]);
                int end = Integer.parseInt(dataArr[6]);
                int bytesToRead = end - start;

                // Send file data back
                try (RandomAccessFile randFile = new RandomAccessFile(file, "r")) {
                    randFile.seek(start);

                    byte[] buffer = new byte[bytesToRead];
                    int bytesRead = randFile.read(buffer);

                    if (bytesRead != -1) {
                        byte[] validBytes = Arrays.copyOf(buffer, bytesRead);
                        String encodedContent = Base64.getEncoder().encodeToString(validBytes);

                        response = String.format("FILE %s OK START %d END %d DATA %s", file, start, end, encodedContent);
                        sendResponse(response, client, socket);
                    } else {
                        System.out.println("End of file reached before reading any bytes.");
                    }
                }
                catch(Exception e){}
            }
            else{
                get = false;
                response = String.format("FILE %s CLOSE_OK", file);
                sendResponse(response, client, socket);
            }
        }
    }
}