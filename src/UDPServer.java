import java.sql.SQLException;
import java.util.logging.*;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
public class UDPServer {
    private final static Logger logger = Logger.getLogger("UDPServer");

    public static void main(String args[]) throws IOException {
        int UDPPort = 9884;
        try (DatagramSocket serverSocket = new DatagramSocket(UDPPort)) {
            try {
                Database db = new Database();

            } catch (SQLException e) {
                logger.warning(e.getMessage());
            }

            //Listen for incoming connections for ever
            while (true) {

                byte[] receiveData = new byte[1024];
                byte[] responseBytes = new byte[1024];
//                System.out.println("This is  UDP server- Waiting for data to receive");
                logger.info("UDP server started at port: " + UDPPort);
                // Create a receive Datagram packet and receive through socket
                DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(requestPacket);

                String requestContent = new String(requestPacket.getData());
                logger.info("Received" + requestContent.length() + ": " + requestContent);


                //Get client attributes from the received data
                InetAddress clientAddressUDP = requestPacket.getAddress();
                int clientPortUDP = requestPacket.getPort();
                String responseContent = requestContent.toUpperCase();
                responseBytes = responseContent.getBytes();

                //Create a send Datagram packet and send through socket
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
                serverSocket.send(responsePacket);

            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
            throw e;
        }

    }
}
