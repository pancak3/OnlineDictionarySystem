import java.net.*;
import java.rmi.UnexpectedException;
import java.sql.SQLException;
import java.util.logging.*;

import java.io.IOException;

import Database.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;

public class UDPServer {
    private final static Logger logger = Logger.getLogger("UDPServer");

    public static void main(String[] args) throws IOException {
        //for print Class-Path
//        final File f = new File(UDPServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
//        logger.info(f.toString());
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
//                System.out.println("This is  UDP server- Waiting for data to receive");
                logger.info("UDP server started at port: " + UDPPort);
                // Create a receive Datagram packet and receive through socket
                DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(requestPacket);

                String requestContent = new String(requestPacket.getData());
                requestContent = "{\"action\":\"query\",\"data\":{\"wordName\":\"apple\"}}";
                logger.info("Received " + requestContent.length() + " length:\r\n   " + requestContent);

                logger.info("handling");

                JSONObject requestJson = new JSONObject();
                try {
                    requestJson = handleRequest(requestContent);

                } catch (ParseException e) {
                    logger.warning(e.getMessage());
                    //tell client its an illegal request
                }
                //push this request in queue
                //and send request received confirmation back

                logger.info("Got legal request: " + requestJson.toJSONString());


                //Get client attributes from the received data
                InetAddress clientAddressUDP = requestPacket.getAddress();
                int clientPortUDP = requestPacket.getPort();
                String responseContent = requestContent.toUpperCase();
                byte[] responseBytes = responseContent.getBytes();

                //Create a send Datagram packet and send through socket
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
                try {
                    serverSocket.send(responsePacket);

                } catch (IOException e) {
                    logger.warning(e.getMessage());
                    throw e;
                }
            }
        } catch (BindException e) {
            logger.warning("Port: " + UDPPort + " -> " + e.getMessage());
            System.exit(0);
        }

    }

    private static JSONObject handleRequest(String requestContent) throws ParseException {
        JSONObject requestJSON;
        try {
            JSONParser parser = new JSONParser();
            requestJSON = (JSONObject) parser.parse(requestContent);

        } catch (ParseException e) {
            logger.warning(e.getMessage());
            throw e;
        }
        return requestJSON;
    }

}
