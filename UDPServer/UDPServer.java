import java.net.*;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import java.io.IOException;

import Database.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;

public class UDPServer {
    private final static Logger logger = Logger.getLogger("UDPServer");
    ;

    public static void main(String[] args) throws IOException {
        //for print Class-Path
//        final File f = new File(UDPServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
//        logger.info(f.toString());
        int UDP_PORT = 9884;
        int QUEUE_SIZE = 20;
        try {
            try {
                Database db = new Database();
            } catch (SQLException e) {
                logger.warning(e.getMessage());
            }

            BlockingQueue<DatagramPacket> requestQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
            BlockingQueue<DatagramPacket> receivedConfirmation = new LinkedBlockingQueue<>(QUEUE_SIZE);

            //Listen for incoming connections for ever
            DatagramSocket serverSocket = new DatagramSocket(UDP_PORT);
            logger.info("UDP server started at port: " + UDP_PORT);

            while (true) {
                byte[] receiveData = new byte[1024];
                // Create a receive Datagram packet and receive through socket
                DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(requestPacket);
                try {
                    requestQueue.put(requestPacket);
                    receivedConfirmation.put(requestPacket);
                    logger.info("Put request and confirmation task in queues");

                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
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
            logger.warning("Port: " + UDP_PORT + " -> " + e.getMessage());
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
