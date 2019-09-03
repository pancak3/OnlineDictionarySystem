import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.util.logging.*;


public class UDPClient {
    private final static Logger logger = Logger.getLogger("UDPClient");
    private final static String UDP_SERVER_ADDR = "localhost";
    private final static int UDP_SERVER_PORT = 7397;

    public static void main(String args[]) throws IOException {

        DatagramSocket clientSocket = null;
        InetAddress ServerAddressUDP = InetAddress.getByName(UDP_SERVER_ADDR);
        int serverPortUDP = UDP_SERVER_PORT;
        try {
            logger.info("UDPClient stated with UDP_SERVER info -> " + UDP_SERVER_ADDR + ":" + UDP_SERVER_PORT);
//            System.out.println("This is UDP Client- Enter some text to send to the UDP server");
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

            // Create a UDP socket object
            clientSocket = new DatagramSocket();
            //IP and port for socket (static method)

            // As UDP Datagrams are bounded by fixed message boundaries, define the length
            byte[] requestBytes;
            byte[] responseBytes = new byte[1024];

            String request = inFromUser.readLine();
            requestBytes = request.getBytes();

            // Create a send Datagram packet and send through socket
            DatagramPacket sendPacket = new DatagramPacket(requestBytes, requestBytes.length, ServerAddressUDP, serverPortUDP);
            clientSocket.send(sendPacket);

            // Create a receive Datagram packet and receive through socket
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);
            clientSocket.receive(responsePacket);
            String responseContent = new String(responsePacket.getData());
            logger.info("Get response from server: " + responseContent);
            //Close the Socket
            clientSocket.close();

        } catch (IOException e) {
            logger.warning(e.getMessage());
            throw e;

        } finally {
            if (clientSocket != null)
                clientSocket.close();

        }

    }


}
