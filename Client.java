import java.net.*;
import java.util.Scanner;
import java.io.*;  // Import the File class
import java.util.Base64;

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
                String response = getResponse(clientSocket);
                String[] dataArr = response.split(" ");
                int size = Integer.parseInt(dataArr[3]);
                port = Integer.parseInt(dataArr[5]);

                // Depending on response go download the file
                downloadFile(file, size, clientSocket, port);
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
            System.out.println("Request sent to server: " + request);
        }
        catch(Exception e){System.out.println(e);}
    }

    public static String getResponse(DatagramSocket clientSocket){
        try{
            byte[] receiveData = new byte[1000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

            System.out.println("Response from server " + response);
            return response;
        }
        catch(Exception e){System.out.println(e);}

        return null;
    }


    private static void downloadFile(String file, int size, DatagramSocket client, int port){
        int bytesToRead = 1000;
        int start = 0;
        int end = bytesToRead;

        String request;

        while(start < size){
            // Send request 
            request = String.format("FILE %s GET START %d END %d", file, start, end);
            sendRequest(request, client, port);
        
            // Get response
            String response = getResponse(client);
            String[] dataArr = response.split(" ");
            //String content = dataArr[8];

            // decode into String from encoded format
            byte[] actualByte = Base64.getDecoder().decode(dataArr[8]);
            String content = new String(actualByte);

            // Write to file
            try (RandomAccessFile randFile = new RandomAccessFile("COPY" + file, "rw")) {
                randFile.seek(start);
                randFile.writeBytes(content);
            }
            catch(Exception e){}

            // Increment start and end
            start = end;
            end +=  bytesToRead;
        }

        request = String.format("FILE %s CLOSE", file);
        sendRequest(request, client, port);
    }
}