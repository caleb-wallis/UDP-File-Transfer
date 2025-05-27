import java.net.*;
import java.util.Scanner;
import java.io.*;  // Import the File class
import java.util.Base64;

public class Client {
    static String hostname;

    public static void main(String[] args) {
        // Ensure the correct number of arguments are provided
        if (args.length != 3) {
            System.out.println("Usage: java Client <hostname> <port> <filename>");
            return;
        }

        hostname = args[0];
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

                // Send download request to the server and receive response
                String response = sendAndReceive(request, clientSocket, port);
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
            byte[] sendData = request.getBytes();
            InetAddress serverAddress = InetAddress.getByName(hostname);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
            clientSocket.send(sendPacket);
            //System.out.println("Request sent to server: " + request);
        }
        catch(Exception e){System.out.println(e);}
    }

    public static String getResponse(DatagramSocket clientSocket) throws SocketTimeoutException, IOException {
        byte[] receiveData = new byte[2000];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        //System.out.println("Response from server " + response);
        return response;
    }

    private static String sendAndReceive(String request, DatagramSocket clientSocket, int port){
        int maxRetries = 5;
        int timeoutMillis = 1000;
        int attempts = 0;

        while (attempts < maxRetries) {
            try{
                // Send Request
                sendRequest(request, clientSocket, port);
                // Set current timeout
                clientSocket.setSoTimeout(timeoutMillis); 
                // Get Response
                return getResponse(clientSocket);
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout occurred (attempt " + (attempts + 1) + ", timeout " + timeoutMillis + "ms). Retrying...");
                timeoutMillis *= 2; // Double the timeout
                attempts++;
            } catch (IOException e) {
                System.out.println("I/O error: " + e.getMessage() + ". Retrying...");
                attempts++;
            } catch (Exception e) {
                System.out.println("Unexpected Error: " + e);
                return null;
            }

        }
        // If fail then throw exception
        System.out.println("No response after " + maxRetries + " attempts.");
        return null;
    }

    private static void downloadFile(String file, int size, DatagramSocket client, int port){
        String request;

        int bytesToRead = 1000;
        int start = 0;
        int end = 999;

        while(start < size){
            if(size < end){
                end = size -1;
            }

            // Send get request and get response
            request = String.format("FILE %s GET START %d END %d", file, start, end);
            String response = sendAndReceive(request, client, port);
            String[] dataArr = response.split(" ");

            String receivedFile = dataArr[1];
            int receivedStart = Integer.parseInt(dataArr[4]);
            int receivedEnd = Integer.parseInt(dataArr[6]);

            // decode into String from encoded format
            byte[] fileBytes = Base64.getDecoder().decode(dataArr[8]);

            // DO CHECKS THAT WE RECEIVED CORRECT PACKET

            // If file is not the same don't write the data
            if(!file.equals(receivedFile)){
                System.out.println("File was different to response");
                continue;
            }

            // If start and end are not the same as the received start and ends
            if(start != receivedStart || end != receivedEnd){
                System.out.println("Start or End was different to response");
                System.out.println(response);
                continue;
            }

            // If we lost some bytes resend request
            if(fileBytes.length != bytesToRead && fileBytes.length + start != size){
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

        // Send close request and get response
        request = String.format("FILE %s CLOSE", file);
        String response = sendAndReceive(request, client, port);
    }
}