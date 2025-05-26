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
                int newPort = Integer.parseInt(dataArr[5]);

                // Depending on response go download the file
                downloadFile(file, size, clientSocket, newPort);
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
            //System.out.println("Request sent to server: " + request);
        }
        catch(Exception e){System.out.println(e);}
    }

    public static String getResponse(DatagramSocket clientSocket){
        try{
            byte[] receiveData = new byte[2000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

            //System.out.println("Response from server " + response);
            return response;
        }
        catch(Exception e){System.out.println(e);}

        return null;
    }


    private static void downloadFile(String file, int size, DatagramSocket client, int port){
        String request;

        int bytesToRead = 1000;
        int start = 0;
        int end = 999;

        if(size < end){
            end = size;
        }

        while(start < size){
            // Send request 
            request = String.format("FILE %s GET START %d END %d", file, start, end);
            sendRequest(request, client, port);
        
            // Get response
            String response = getResponse(client);
            String[] dataArr = response.split(" ");

            String recivedFile = dataArr[1];
            int recievedStart = Integer.parseInt(dataArr[4]);
            int recievedEnd = Integer.parseInt(dataArr[6]);

            // decode into String from encoded format
            byte[] fileBytes = Base64.getDecoder().decode(dataArr[8]);

            // DO CHECKS THAT WE RECIEVED CORRECT PACKET

            // If file is not the same don't write the data
            if(!file.equals(recivedFile)){
                System.out.println("File was different to response");
                continue;
            }

            // If start and end are not the same as the recieved start and ends
            if(start != recievedStart || end != recievedEnd){
                System.out.println("Start or End was different to response");
                System.out.println(response);
                continue;
            }

            // If we lost some bytes resend request
            if(fileBytes.length + start != end && fileBytes.length + start != size){
                System.out.println("File Bytes != BytesToRead");
                System.out.println(fileBytes.length);
                System.out.println(size);
                continue;
            }

            // Write to file
            try (RandomAccessFile randFile = new RandomAccessFile("COPY" + file, "rw")) {
                // Write raw bytes to file
                randFile.seek(start);
                randFile.write(fileBytes);
            }
            catch(Exception e){}

            // Increment start and end
            start += bytesToRead;
            end += bytesToRead;
        }

        request = String.format("FILE %s CLOSE", file);
        sendRequest(request, client, port);

        // Get Close Response
        getResponse(client);
    }
}