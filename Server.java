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
                    response = String.format("OK %s %d %d", file, f.length(), threadSocket.getPort());

                    // Create thread with new socket and file
                    new Thread(() -> sendFile(f, client, threadSocket)).start();
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

    private static void sendFile(File file, DatagramPacket client, DatagramSocket socket){
        System.out.println("Thread was made");
        boolean get = true;

        while(get){
            // Get download request
            String request = getRequest(client, socket);
            String[] dataArr = request.split(" ");

            // Check 3rd value for CLOSE or GET
            String operation = dataArr[3];

            // Perform Operation
            String response = "";

            if(operation.equals("GET")){
                int start = Integer.parseInt(dataArr[4]);
                int end = Integer.parseInt(dataArr[6]);

                // Send file data back
                int startPosition = start; // Read from the 11th byte
                int bytesToRead = end - start;

                try (RandomAccessFile randFile = new RandomAccessFile(file.getName(), "r")) {
                    randFile.seek(startPosition);

                    byte[] buffer = new byte[bytesToRead];
                    int bytesRead = randFile.read(buffer);

                    if (bytesRead != -1) {
                        String content = new String(buffer, 0, bytesRead);
                        response = String.format("FILE %s OK START %d END %d DATA %s", file.getName(), start, end, content);
                        sendResponse(response, client, socket);
                    } else {
                        System.out.println("End of file reached before reading any bytes.");
                    }
                }
                catch(Exception e){}
            }
            else{
                get = false;
                response = String.format("FILE %s CLOSE_OK", file.getName());
                sendResponse(response, client, socket);
            }
        }
    }
}