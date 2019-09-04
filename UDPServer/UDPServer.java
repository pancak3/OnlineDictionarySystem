import java.net.*;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;
import java.io.IOException;

import Database.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//to remove warning both in IJ and compile, but guess there is better way to use Json
@SuppressWarnings("unchecked")

public class UDPServer {
    private final static int UDP_PORT = 7397;
    private final static int QUEUE_SIZE = 1000;
    private final static int HANDLER_POOL_SIZE = 200;
    private final static int CONFIRMOR_POOL_SIZE = 5;
    private final static int RESPONDER_POOL_SIZE = 5;
    private final static int TASK_RESPOND_CYCLE_MILLIS = 100;
    private final static int MAX_RESPOND_TIMES = 100;
    private final static int MAX_BUFFER_SIZE = 10240;


    static class ResponseTask {
        DatagramPacket respondPacket;
        long timestamp;
        int handledTimes;

        ResponseTask(DatagramPacket respondPacket, long timestamp, int handledTimes) {
            this.respondPacket = respondPacket;
            this.timestamp = timestamp;
            this.handledTimes = handledTimes;
        }
    }

    private final static BlockingQueue<DatagramPacket> requestQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> HandleRequestFailQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<ResponseTask> respondQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> requestConfirmationQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static BlockingQueue<DatagramPacket> respondConfirmationQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final static CopyOnWriteArrayList<String> respondConfirmationList = new CopyOnWriteArrayList<String>();
    private final static Database db = new Database();

    private final static Logger logger = Logger.getLogger("UDPServer");

    public static void main(String[] args) throws IOException {
        // start receiver
        new Thread(new Receiver(), "Receiver").start();
    }


    static class Receiver implements Runnable {
        private final static Logger logger = Logger.getLogger("Receiver");

        public void run() {
            try {

                DatagramSocket receiverSocket = new DatagramSocket(UDP_PORT);
                logger.info("UDP server started at port: " + UDP_PORT);

                // start confirmor pool
                for (int i = 0; i < CONFIRMOR_POOL_SIZE; i++) {

                    new Thread(new Confirmor(), "Confirmor-" + i).start();
                }
                //start responder pool
                for (int i = 0; i < RESPONDER_POOL_SIZE; i++) {

                    new Thread(new Responder(), "Responder-").start();
                }

                // start handler pool
                for (int i = 0; i < HANDLER_POOL_SIZE; i++) {
                    new Thread(new Handler(), "Handler-" + i).start();
                }

                while (true) {
                    byte[] receiveData = new byte[MAX_BUFFER_SIZE];
                    // Create a receive Datagram packet and receive through socket
                    DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);

                    try {
                        receiverSocket.receive(requestPacket);
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


    static class Handler implements Runnable {
        private final static Logger logger = Logger.getLogger("Handler");


        public void run() {
            try {
                logger.info(Thread.currentThread().getName() + " is working ...");
                while (true) {
                    DatagramPacket requestPacket = requestQueue.take();
                    //Get client attributes from the received data
                    InetAddress clientAddressUDP = requestPacket.getAddress();
                    int clientPortUDP = requestPacket.getPort();
                    String requestContent = new String(requestPacket.getData()).substring(0, requestPacket.getLength());

                    JSONObject respondJson = (JSONObject) new JSONObject();
                    byte[] responseBytes;

                    DatagramPacket responsePacket;
                    try {
                        //handle request

                        logger.info("Handling: " + requestContent);
                        handleRequest(requestPacket);


                    } catch (ParseException e) {
                        respondJson.put("respondData", "Illegal request.");
                        respondJson.put("status", "failed");
                        responseBytes = respondJson.toJSONString().getBytes();
                        responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
                        HandleRequestFailQueue.put(responsePacket);
                    } catch (Exception e) {
                        logger.warning("[*] Unexpect exception: " + e.getMessage());

                        respondJson.put("respondData", "Server err");
                        respondJson.put("status", "failed");
                        responseBytes = respondJson.toJSONString().getBytes();
                        responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
                        HandleRequestFailQueue.put(responsePacket);
                    }


                }
            } catch (InterruptedException e) {
                logger.warning(e.getMessage());
            }
        }

        private static void handleRequest(DatagramPacket requestPacket) throws ParseException {
            JSONParser parser = new JSONParser();


            String requestContent = new String(requestPacket.getData()).substring(0, requestPacket.getLength());

            JSONObject requestJSON;
            try {
                requestJSON = (JSONObject) parser.parse(requestContent);

                if (requestJSON.containsKey("action") && requestJSON.containsKey("data")) {
                    //requestJSON.get("action") without (String) is not allowed
                    //I should learn more Java
                    //need to understand the principle of this
                    String action = (String) requestJSON.get("action");
                    JSONObject data = (JSONObject) parser.parse(requestJSON.get("data").toString());

                    JSONObject respondJson = (JSONObject) new JSONObject();
                    InetAddress clientAddressUDP = requestPacket.getAddress();
                    int clientPortUDP = requestPacket.getPort();

                    byte[] responseBytes;
                    String hashString = requestJSON.toJSONString() + String.valueOf(System.currentTimeMillis());
                    respondJson.put("requestHashCode", hashString.hashCode());

                    //each should check and put status in confirmationQueue
                    switch (action) {
                        case "query":
                            if (data.containsKey("wordName")) {
                                respondJson.put("data", query(data));
                            } else {
                                throw new ParseException(-1);
                            }
                            break;
                        case "add":
                            if (data.containsKey("wordName") && data.containsKey("wordType") && data.containsKey("wordMeaning")) {

                                respondJson.put("data", add(data));
                            } else {
                                throw new ParseException(-1);
                            }
                            break;

                        case "edit":
                            if (data.containsKey("idx") && data.containsKey("wordName") && data.containsKey("wordType") && data.containsKey("wordMeaning")) {
                                respondJson.put("data", edit(data));

                            } else {
                                throw new ParseException(-1);
                            }
                            break;

                        case "remove":
                            if (data.containsKey("idx")) {
                                respondJson.put("data", remove(data));
                            } else {
                                throw new ParseException(-1);
                            }
                            break;

                        case "confirm":
                            if (data.containsKey("requestHashCode")) {
                                respondConfirmationList.add(data.get("requestHashCode").toString());
                            }
                            break;

                    }

                    if (respondJson.containsKey("data")) {
                        respondJson.put("status", "success");

                        String respondContent = respondJson.toString();
                        responseBytes = respondContent.getBytes();
                        //Create a send Datagram packet and send through socket
                        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);

                        ResponseTask respondTask = new ResponseTask(responsePacket, System.currentTimeMillis(), 0);
                        respondTask.respondPacket = responsePacket;
                        respondQueue.put(respondTask);
                    }

                } else {
                    throw new ParseException(-1);
                }

            } catch (ParseException | InterruptedException e) {
                if (e.getMessage() == null) {
                    logger.warning("Illegal request: " + requestContent);

                } else {
                    logger.warning(e.getMessage());
                }

            }
        }

        private static JSONObject query(JSONObject data) throws ParseException {
            String wordName = data.get("wordName").toString();
            try {
                return db.queryWord(wordName);
            } catch (SQLException e) {
                logger.warning(Thread.currentThread().getName() + " while querying " + wordName + e.getMessage());
                //err or no word match in database
                throw new ParseException(-1);
            }
        }

        private static JSONObject add(JSONObject data) throws ParseException {
            String wordName = data.get("wordName").toString();
            String wordType = data.get("wordType").toString();
            String wordMeaning = data.get("wordMeaning").toString();
            try {
                return db.addWord(wordName, wordType, wordMeaning);
            } catch (SQLException e) {
                logger.warning(Thread.currentThread().getName() + " while adding " + wordName + e.getMessage());
                //err or no word match in database
                throw new ParseException(-1);
            }
        }

        private static JSONObject edit(JSONObject data) throws ParseException {
            String idx = data.get("idx").toString();
            String wordName = data.get("wordName").toString();
            String wordType = data.get("wordType").toString();
            String wordMeaning = data.get("wordMeaning").toString();
            try {
                return db.editWord(Integer.parseInt(idx), wordName, wordType, wordMeaning);
            } catch (SQLException e) {
                logger.warning(Thread.currentThread().getName() + " while editing " + wordName + e.getMessage());
                //err or no word match in database
                throw new ParseException(-1);
            }
        }


        private static JSONObject remove(JSONObject data) throws ParseException {
            String idx = data.get("idx").toString();
            try {
                return db.removeWord(Integer.parseInt(idx));
            } catch (SQLException e) {
                logger.warning(Thread.currentThread().getName() + " while remove " + idx + e.getMessage());
                //err or no word match in database
                throw new ParseException(-1);
            }
        }

    }

    static class Confirmor implements Runnable {
        private final static Logger logger = Logger.getLogger("Confirmor");

        public void run() {
            try {

                DatagramSocket confirmorSocket = new DatagramSocket();
                logger.info("Confirmor started. ");

                while (true) {

                    DatagramPacket requestPacket = requestConfirmationQueue.take();
                    //Get client attributes from the received data
                    logger.info("[*] Handling confirmation.");
                    InetAddress clientAddressUDP = requestPacket.getAddress();
                    int clientPortUDP = requestPacket.getPort();

                    JSONObject confirmationJson = new JSONObject();
                    String requestContent = new String(requestPacket.getData()).substring(0, requestPacket.getLength());

                    confirmationJson.put("status", "received");
                    byte[] confirmationBytes = confirmationJson.toJSONString().getBytes();
                    //Create a send Datagram packet and send through socket
                    DatagramPacket confirmationPacket = new DatagramPacket(confirmationBytes, confirmationBytes.length, clientAddressUDP, clientPortUDP);
                    try {
                        confirmorSocket.send(confirmationPacket);
                        logger.info("[*] Send confirmation: " + confirmationJson.toJSONString());
                    } catch (IOException e) {
                        logger.warning("Error while send confirmation: " + e.getMessage());
                    }
                }
            } catch (SocketException e) {
                logger.warning("Failed to start confirmor" + e.getMessage());
                System.exit(0);
            } catch (InterruptedException e) {
                logger.warning("Receiver was interrupted.");
            }
        }
    }

    static class Responder implements Runnable {
        private final static Logger logger = Logger.getLogger("Responder");

        public void run() {
            try {

                DatagramSocket responderSocket = new DatagramSocket();
                logger.info("Responder started. ");
                JSONParser parser = new JSONParser();

                while (true) {

                    ResponseTask responseTask = respondQueue.take();
                    if (responseTask.handledTimes == 0 || System.currentTimeMillis() - responseTask.timestamp > TASK_RESPOND_CYCLE_MILLIS) {
                        if (responseTask.handledTimes > MAX_RESPOND_TIMES) {
                            //Exceeded max response times, remove it by not putting back in.
                            logger.warning("Response task times exceeded " + MAX_RESPOND_TIMES + " times, will remove : " + new String(responseTask.respondPacket.getData()));
                        } else {
                            String responseContent = new String(responseTask.respondPacket.getData());
                            try {
                                JSONObject respondJson = (JSONObject) parser.parse(responseContent);
                                String originRequestHashCode = respondJson.get("requestHashCode").toString();
                                if (respondConfirmationList.contains(originRequestHashCode)) {
                                    respondConfirmationList.remove(originRequestHashCode);
                                    //respond task taken but did't put back in == remove
                                    logger.info("Respond task was confirmed: " + responseContent);
                                } else {
                                    try {
                                        responderSocket.send(responseTask.respondPacket);
                                        //put respond packet back in
                                        responseTask.handledTimes += 1;
                                        responseTask.timestamp = System.currentTimeMillis();
                                        respondQueue.put(responseTask);
//                                        logger.info("Responded: " + responseContent);

                                    } catch (IOException e) {
                                        logger.warning("Error while responding: " + e.getMessage());
                                    }
                                }

                            } catch (ParseException e) {
                                logger.info("Error while extract respond packet.(Dare ever reach here)" + e.getMessage());
                            }
                        }

                    } else {
                        //Just handled, put it back in.
                        respondQueue.put(responseTask);
                    }


                }
            } catch (SocketException e) {
                logger.warning("Failed to start responder" + e.getMessage());
                System.exit(0);
            } catch (InterruptedException e) {
                logger.warning("Responder was interrupted.");
            }
        }
    }

}
