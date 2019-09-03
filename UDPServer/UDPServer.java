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
    private final static BlockingQueue<DatagramPacket> requestIllegalQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> respondQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> requestConfirmationQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> respondConfirmationQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static Database db = new Database();

    private final static Logger logger = Logger.getLogger("UDPServer");

    public static void main(String[] args) throws IOException {
        // start receiver
        new Thread(new receiver(), "Receiver").start();
        // start handler pool
        for (int i = 0; i < HANDLER_POOL_SIZE; i++) {
            new Thread(new handler(), "Handler-" + i).start();
        }
        // start confirmor pool
        for (int i = 0; i < CONFIRMOR_POOL_SIZE; i++) {
            new Thread(new confirmor(), "Confirmor-" + i).start();
        }


    }

    static class receiver implements Runnable {
        private final static Logger logger = Logger.getLogger("receiver");

        public void run() {
            try {

                DatagramSocket serverSocket = new DatagramSocket(UDP_PORT);
                logger.info("UDP server started at port: " + UDP_PORT);

                while (true) {
                    byte[] receiveData = new byte[1024];
                    // Create a receive Datagram packet and receive through socket
                    DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);

                    try {
                        serverSocket.receive(requestPacket);
                        requestQueue.put(requestPacket);
                        requestConfirmationQueue.put(requestPacket);
                        logger.info("Put request and confirmation task in queues");
                    } catch (IOException e) {
                        logger.warning(e.getMessage());
                    } catch (Exception e) {
                        logger.warning(e.getMessage());
                    }
                }
            } catch (SocketException e) {
                logger.warning("Port: " + UDP_PORT + " -> " + e.getMessage());
                System.exit(0);
            }
        }
    }

    static class handler implements Runnable {
        private final static Logger logger = Logger.getLogger("handler");

        public void run() {
            try {
                logger.info(Thread.currentThread().getName() + " is working ...");
                while (true) {
                    DatagramPacket requestPacket = requestQueue.take();
                    //Get client attributes from the received data
                    InetAddress clientAddressUDP = requestPacket.getAddress();
                    int clientPortUDP = requestPacket.getPort();
                    String requestContent = new String(requestPacket.getData());
                    JSONObject respondJson = (JSONObject) new JSONObject();
                    respondJson.put("requestHashCode", requestContent.hashCode());
                    byte[] responseBytes = new byte[1024];
                    DatagramPacket responsePacket;
                    try {
                        //handle request

                        respondJson.put("respondData", handleRequest(requestContent));
                        respondJson.put("status", "success");

                        logger.info(Thread.currentThread().getName() + " got legal request: " + respondJson.toJSONString());

                        requestContent = respondJson.toString();

                        //debug area>>>>>>>
                        requestContent = "{\"action\":\"query\",\"data\":{\"wordName\":\"apple\"}}";
                        logger.info("Handling: " + requestContent);
                        String responseContent = requestContent.toUpperCase();
                        //<<<<<<<
                        responseBytes = responseContent.getBytes();
                        //Create a send Datagram packet and send through socket
                        responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
//                  serverSocket.send(responsePacket);
                        respondQueue.put(responsePacket);
                    } catch (ParseException e) {
                        respondJson.put("respondData", "Illegal request.");
                        respondJson.put("status", "failed");

                        responseBytes = respondJson.toJSONString().getBytes();
                        responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
                        requestIllegalQueue.put(responsePacket);
                    }


                }
            } catch (InterruptedException e) {
                logger.warning(e.getMessage() + " here");
            }
        }

        private static JSONObject handleRequest(String requestContent) throws ParseException {
            JSONObject requestJSON;
            try {
                JSONParser parser = new JSONParser();
                requestJSON = (JSONObject) parser.parse(requestContent);
                if (requestJSON.containsKey("action") && requestJSON.containsKey("data")) {
                    String action = requestJSON.get("action").toString();
                    JSONObject data = (JSONObject) requestJSON.get("data");

                    //each should check and put status in confirmationQueue
                    switch (action) {
                        case "query":
                            if (data.containsKey("wordName")) {
                                query(data);
                            } else {
                                throw new ParseException(-1);
                            }
                            break;
                        case "add":
                            if (data.containsKey("wordName") && data.containsKey("meaning")) {
                                add(data);
                            } else {
                                throw new ParseException(-1);
                            }
                            break;
                        case "edit":
                            if (data.containsKey("idx") && data.containsKey("wordName") && data.containsKey("meaning")) {
                                edit(data);
                            } else {
                                throw new ParseException(-1);
                            }
                            break;
                        case "remove":
                            if (data.containsKey("idx") && data.containsKey("wordName")) {
                                remove(data);
                            } else {
                                throw new ParseException(-1);
                            }
                            break;
                    }
                } else {
                    throw new ParseException(-1);
                }

            } catch (ParseException e) {
                if (e.getMessage() == null) {
                    logger.warning("Illegal request: " + requestContent);

                } else {
                    logger.warning(e.getMessage());
                }
                throw e;

            }
            return requestJSON;
        }

        private static void illegalRequest(JSONObject data) {

        }

        private static void query(JSONObject data) {

        }

        private static void add(JSONObject data) {

        }

        private static void edit(JSONObject data) {

        }

        private static void remove(JSONObject data) {

        }
    }

    static class confirmor implements Runnable {
        private final static Logger logger = Logger.getLogger("confirmor");

        public void run() {

        }
    }
}
