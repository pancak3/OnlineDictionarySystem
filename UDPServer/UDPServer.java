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
    private final static int QUEUE_SIZE = 42;
    private final static int HANDLER_POOL_SIZE = 10;
    private final static int CONFIRMOR_POOL_SIZE = 10;
    private final static int TASK_RESPOND_CYCLE_MILLIS = 100;
    private final static int MAX_RESPOND_TIMES = 30;

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
        private final static Logger logger = Logger.getLogger("receiver");

        public void run() {
            try {

                DatagramSocket receiverSocket = new DatagramSocket(UDP_PORT);
                logger.info("UDP server started at port: " + UDP_PORT);

                // start confirmor pool
                new Thread(new Confirmor(), "Confirmor-").start();
                //start responder
//                new Thread(new Responder(), "Responder-").start();

                // start handler pool
//                for (int i = 0; i < HANDLER_POOL_SIZE; i++) {
//                    new Thread(new Handler(), "Handler-" + i).start();
//                }

                while (true) {
                    byte[] receiveData = new byte[1024];
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
                    respondJson.put("requestHashCode", requestContent.hashCode());
                    byte[] responseBytes;
                    DatagramPacket responsePacket;
                    try {
                        //handle request
//                        requestContent = "{\"action\":\"query\",\"data\":{\"wordName\":\"banana\"}}";


                        respondJson.put("respondData", handleRequest(requestContent));
                        logger.info("Handling: " + requestContent);
                        respondJson.put("status", "success");
                        String respondContent = respondJson.toString();
                        responseBytes = respondContent.getBytes();

                        //Create a send Datagram packet and send through socket
                        responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddressUDP, clientPortUDP);
                        ResponseTask respondTask = new ResponseTask(responsePacket, System.currentTimeMillis(), 0);
                        respondTask.respondPacket = responsePacket;
                        respondQueue.put(respondTask);
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

        private static JSONObject handleRequest(String requestContent) throws ParseException {
            JSONObject requestJSON;
            try {
                JSONParser parser = new JSONParser();
                requestJSON = (JSONObject) parser.parse(requestContent);
                if (requestJSON.containsKey("action") && requestJSON.containsKey("data")) {
                    //requestJSON.get("action") without (String) is not allowed
                    //I should learn more Java
                    //need to understand the principle of this
                    String action = (String) requestJSON.get("action");

                    //I jump, who else?
                    JSONObject data = (JSONObject) parser.parse(requestJSON.get("data").toString());

                    //each should check and put status in confirmationQueue
                    switch (action) {
                        case "query":
                            if (data.containsKey("wordName")) {
                                return query(data);
                            } else {
                                throw new ParseException(-1);
                            }
                        case "add":
                            if (data.containsKey("wordName") && data.containsKey("wordType") && data.containsKey("meaning")) {
                                return add(data);
                            } else {
                                throw new ParseException(-1);
                            }
                        case "edit":
                            if (data.containsKey("idx") && data.containsKey("wordName") && data.containsKey("wordType") && data.containsKey("meaning")) {
                                return edit(data);
                            } else {
                                throw new ParseException(-1);
                            }
                        case "remove":
                            if (data.containsKey("idx") && data.containsKey("wordName")) {
                                return remove(data);
                            } else {
                                throw new ParseException(-1);
                            }
                        case "confirmation":
                            if (data.containsKey("respondHash")) {
                                return addConfirmation(data);
                            } else {
                                throw new ParseException(-1);
                            }
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
            String meaning = data.get("meaning").toString();
            try {
                return db.editWord(Integer.parseInt(idx), wordName, wordType, meaning);
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

        private static JSONObject addConfirmation(JSONObject data) {
            String respondHash = data.get("respondHash").toString();
            respondConfirmationList.add(respondHash);
            return new JSONObject();
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
                    logger.info("[*] Received :" + requestContent);
                    confirmationJson.put("status", "received");
                    confirmationJson.put("requestHashCode", requestContent.hashCode());
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

    static class FailedResponder implements Runnable {
        private final static Logger logger = Logger.getLogger("FailedResponder");

        public void run() {
            try {

                DatagramSocket responderSocket = new DatagramSocket();
                logger.info("FailedResponder started. ");

                while (true) {

                    DatagramPacket responsePacket = HandleRequestFailQueue.take();

                    try {
                        responderSocket.send(responsePacket);

                    } catch (IOException e) {
                        logger.warning("Failed to send request failed packet");
                        HandleRequestFailQueue.put(responsePacket);
                    }


                }
            } catch (SocketException e) {
                logger.warning("Failed to start FailedResponder" + e.getMessage());
                System.exit(0);
            } catch (InterruptedException e) {
                logger.warning("FailedResponder was interrupted.");
            }
        }
    }

}
