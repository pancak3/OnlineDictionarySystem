import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

import java.io.IOException;

import Database.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class UDPServer {
    private final static int UDP_PORT = 7397;
    private final static int QUEUE_SIZE = 42;
    private final static int HANDLER_POOL_SIZE = 10;
    private final static int CONFIRMOR_POOL_SIZE = 10;
    private final static BlockingQueue<DatagramPacket> requestQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> respondQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> requestConfirmationQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> respondConfirmationQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static Database db = new Database();

    private final static Logger logger = Logger.getLogger("UDPServer");

    public static void main(String[] args) throws IOException {

        for (int i = 0; i < HANDLER_POOL_SIZE; i++) {
            new Thread(new handler(), "Handler-" + i).start();
        }
        for (int i = 0; i < CONFIRMOR_POOL_SIZE; i++) {
            new Thread(new confirmor(), "Confirmor-" + i).start();
        }
        try {

            DatagramSocket serverSocket = new DatagramSocket(UDP_PORT);
            logger.info("UDP server started at port: " + UDP_PORT);

            while (true) {
                byte[] receiveData = new byte[1024];
                // Create a receive Datagram packet and receive through socket
                DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(requestPacket);
                try {
                    requestQueue.put(requestPacket);
                    requestConfirmationQueue.put(requestPacket);
                    logger.info("Put request and confirmation task in queues");
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
            }
        } catch (BindException e) {
            logger.warning("Port: " + UDP_PORT + " -> " + e.getMessage());
            System.exit(0);
        }

    }


    static class handler implements Runnable {
        private final static Logger logger = Logger.getLogger("handler");

        public void run() {
            try {
                logger.info(Thread.currentThread().getName() + " is working ...");
                while (true) {

                    try {
                        DatagramPacket requestPacket = requestQueue.take();
                        String requestContent = new String(requestPacket.getData());
                        requestContent = "{\"action\":\"query\",\"data\":{\"wordName\":\"apple\"}}";
                        logger.info("Handling: " + requestContent);

                        //handleRequest() should return respond in Json format
                        JSONObject respondJson = handleRequest(requestContent);
                        logger.info("Got legal request: " + respondJson.toJSONString());

                        //Get client attributes from the received data
                        InetAddress clientAddressUDP = requestPacket.getAddress();
                        int clientPortUDP = requestPacket.getPort();
                        String responseContent = requestContent.toUpperCase();
                        byte[] responseBytes = responseContent.getBytes();

                        //Create a send Datagram packet and send through socket
                        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
//                  serverSocket.send(responsePacket);
                        respondQueue.put(responsePacket);
                    } catch (ParseException e) {
                        logger.warning(e.getMessage());
                    }


                }
            } catch (InterruptedException e) {
                logger.warning(e.getMessage());
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

    static class confirmor implements Runnable {
        private final static Logger logger = Logger.getLogger("confirmor");

        public void run() {

        }


    }
}
