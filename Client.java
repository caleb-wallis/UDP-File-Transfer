import java.net.*;
import java.util.Scanner;
import java.io.*;  // Import the File class
import java.util.Base64;

/**
 * UDP Client that reads file names from input, sends download requests to the server,
 * and retrieves file contents in chunks with error handling and retries.
 */
public class Client {
    static String hostname;

    /**
     * Entry point of the client application.
     *
     * @param args command-line arguments: <hostname> <port> <filename>
     */
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

            System.out.println("Client is ready to download files");

            // Request to download files from filename
            Scanner scanner = new Scanner(downloadFile);
           
            while (scanner.hasNextLine()) {
                // Get file to download
                String file = scanner.nextLine();
                String request = String.format("DOWNLOAD %s", file);

                // Send download request to the server and receive response
                String response = sendAndReceive(request, clientSocket, port);
                String[] dataArr = response.split(" ");

                // Depending on response go download the file
                String download = dataArr[0];
                if(download.equals("OK")){
                    int size = Integer.parseInt(dataArr[3]);
                    int newPort = Integer.parseInt(dataArr[5]);
                    downloadFile(file, size, clientSocket, newPort);
                }
            }

            scanner.close();

        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    /**
     * Sends a UDP request to the server.
     *
     * @param request The string message to send.
     * @param clientSocket The DatagramSocket used for sending.
     * @param port The destination port number.
     */
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

    /**
     * Waits to receive a response from the server over UDP.
     *
     * @param clientSocket The DatagramSocket used for communication.
     * @return The string content of the response from the server.
     */
    public static String getResponse(DatagramSocket clientSocket) throws SocketTimeoutException, IOException {
        byte[] receiveData = new byte[2000];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        //System.out.println("Response from server " + response);
        return response;
    }

    /**
     * Sends a request and waits for a response using retries and exponential backoff on timeout.
     *
     * @param request The message to send.
     * @param clientSocket The UDP socket used for communication.
     * @param port The port to send the request to.
     * @return The response string from the server.
     */
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
                System.out.println("Unkown error: " + e.getMessage() + ". Retrying...");
                attempts++;
            }                  
        }
        // If fail then throw exception
        throw new RuntimeException("No response after " + maxRetries + " attempts.");
    }

    /**
     * Downloads a file in chunks using UDP, verifies integrity, and writes it.
     *
     * @param file The filename to download.
     * @param size The total size of the file.
     * @param client The DatagramSocket used for communication.
     * @param port The port to communicate with.
     */
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
                System.out.println("Start or End was different to what was sent");
                continue;
            }

            // If we lost some bytes resend request
            if(fileBytes.length != bytesToRead && fileBytes.length + start != size){
                System.out.println("File Bytes != BytesToRead");
                System.out.println(fileBytes.length);
                System.out.println(start);
                System.out.println(size);
                continue;
            }

            // Write to file
            try (RandomAccessFile randFile = new RandomAccessFile("COPY" + file, "rw")) {
                // Write raw bytes to file
                randFile.seek(start);
                randFile.write(fileBytes);
            }
            catch(Exception e){
                System.out.println("Couldn't write to file");
                break;
            }

            System.out.println(String.format("File: %s, Downloaded: %d%%", file, (end*100)/(size-1)));

            // Increment start and end
            start += bytesToRead;
            end += bytesToRead;
        }

        // Send close request and get response
        request = String.format("FILE %s CLOSE", file);
        String response = sendAndReceive(request, client, port);
    }
}