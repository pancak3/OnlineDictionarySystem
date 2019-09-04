import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.concurrent.atomic.*;

import java.util.logging.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.print.DocFlavor;
import javax.swing.*;

//to remove warning both in IJ and compile, but guess there is better way to use Json
@SuppressWarnings("unchecked")

public class UDPClient {
    private final static String UDP_SERVER_ADDR = "localhost";
    private final static int UDP_SERVER_PORT = 7397;

    private final static int REQUEST_SEND_CYCLE_MILLIS = 300;
    private final static int MAX_SEND_TIMES = 10;

    private final static int RESPONSE_RECEIVE_TIMEOUT_MILLIS = 10000;

    private final static JSONObject requestJson = (JSONObject) new JSONObject();

    private final static Logger logger = Logger.getLogger("UDPClient");

    public static class Action {
        //not sure about how the atomic works, need to read docs
        private final AtomicReference<JSONObject> actionJson = new AtomicReference<JSONObject>();
        private final JSONObject emptyAction = (JSONObject) new JSONObject();

        public void updateAction(JSONObject updateJson) {
            updateJson.put("timestamp", System.currentTimeMillis());
            updateJson.put("requestTimes", Integer.parseInt(actionJson.get().get("requestTimes").toString()) + 1);
            actionJson.set(updateJson);
        }

        public JSONObject getAction() {
            return actionJson.get();
        }

        public void clearAction() {
            emptyAction.put("timestamp", System.currentTimeMillis());
            emptyAction.put("requestTimes", -1);
            actionJson.set(this.emptyAction);
        }

        Action() {
            this.clearAction();
        }
    }

    public static void main(String args[]) throws IOException {

        DatagramSocket clientSocket = null;
        InetAddress ServerAddressUDP = InetAddress.getByName(UDP_SERVER_ADDR);
        int serverPortUDP = UDP_SERVER_PORT;

        logger.info("UDPClient stated with UDP_SERVER info -> " + UDP_SERVER_ADDR + ":" + UDP_SERVER_PORT);

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
//        String request = userInput.readLine();

        SingleClient mainSingleClient = new SingleClient();
        JSONObject temJson = new JSONObject();
        JSONObject userInputJson = new JSONObject();
        temJson.put("wordName", "banana");
        userInputJson.put("data", temJson);
        userInputJson.put("action", "query");
        userInputJson.put("timestamp", System.currentTimeMillis());
        mainSingleClient.action.updateAction(userInputJson);
        mainSingleClient.run();

    }


    private static class SingleClient implements Runnable {
        private final static String currentThreadName = Thread.currentThread().getName();
        private final static Logger logger = Logger.getLogger("SingleClient-" + currentThreadName);
        private final Action action = new Action();
        private final JSONParser parser = new JSONParser();
        private boolean wasRequestReceived = false;

        public void run() {
            logger.info("[*] Running SingleClient-" + currentThreadName);
            try {
                DatagramSocket singleClientSocket = new DatagramSocket();
                singleClientSocket.setSoTimeout(REQUEST_SEND_CYCLE_MILLIS);

                JSONObject actionJson = this.action.getAction();
                int actionRequestTimes = Integer.parseInt(actionJson.get("requestTimes").toString());

                //make sure request was received by UDPServer
                while (actionRequestTimes <= MAX_SEND_TIMES) {

                    actionJson = this.action.getAction();
                    actionRequestTimes = Integer.parseInt(actionJson.get("requestTimes").toString());
                    try {
                        JSONObject requestJson = (JSONObject) new JSONObject();
                        requestJson.put("action", actionJson.get("action").toString());
                        requestJson.put("data", actionJson.get("data").toString());

                        String requestString = requestJson.toJSONString();
                        byte[] requestBytes = requestString.getBytes();

                        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(UDP_SERVER_ADDR), UDP_SERVER_PORT);

                        if (!wasRequestReceived) {
                            singleClientSocket.send(requestPacket);
                        }

                        //set receive time out

                        byte[] buffer = new byte[1024];
                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);


                        try {
                            singleClientSocket.receive(responsePacket);
                            String responseContent = new String(responsePacket.getData()).substring(0, responsePacket.getLength());

                            try {
                                JSONObject responseJson = (JSONObject) parser.parse(responseContent);
                                if (responseJson.containsKey("status")) {
                                    String requestStatus = (String) responseJson.get("status").toString();
                                    switch (requestStatus) {
                                        case "received":
                                            logger.info("[*] request received by UDPServer");
                                            this.wasRequestReceived = true;
                                            break;
                                        case "success":
                                            logger.info("[*] request success.");

                                            //send UDPServer response received confirmation
                                            JSONObject confirmationJson = (JSONObject) new JSONObject();
                                            JSONObject confirmationDataJson = (JSONObject) new JSONObject();
                                            confirmationDataJson.put("requestHashCode", responseJson.get("requestHashCode"));
                                            confirmationJson.put("action", "confirm");
                                            confirmationJson.put("data", confirmationDataJson);

                                            String confirmationContent = confirmationJson.toJSONString();

                                            byte[] confirmationBytes = confirmationContent.getBytes();
                                            DatagramPacket confirmationPacket = new DatagramPacket(confirmationBytes, confirmationBytes.length, InetAddress.getByName(UDP_SERVER_ADDR), UDP_SERVER_PORT);

                                            singleClientSocket.send(confirmationPacket);
                                            handleResponse(responseJson);

                                            actionRequestTimes = MAX_SEND_TIMES + 1;
                                            action.clearAction();
                                            break;
                                    }
                                }


                            } catch (ParseException e) {
                                logger.warning("Received illegal response :" + responseContent);

                            }

                        } catch (SocketTimeoutException e) {
                            logger.info("[*] " + currentThreadName + " receive timeout: " + e.getMessage());
                        }

//                        logger.info("[*] tried " + actionRequestTimes + " times.");

                        action.updateAction(actionJson);

                    } catch (IOException e) {
                        logger.warning("Error while responding: " + e.getMessage());
                    }

                }


//                singleClientSocket.setSoTimeout(RESPONSE_RECEIVE_TIMEOUT_MILLIS);
//                try {
//                    JSONObject responseJson = (JSONObject) parser.parse(responseContent);
//                    String requestStatus = (String) responseJson.get("status").toString();
//                    switch (requestStatus) {
//                        case "received":
//                            logger.info("[*] request arrived.");
//                            actionJson.put("requestTimes", MAX_SEND_TIMES + 1);
//                            break;
//                        case "success":
//                            logger.info("[*] request success.");
//                            actionJson.put("requestTimes", MAX_SEND_TIMES + 1);
//                            handleResponse(responseJson);
//                            break;
//                    }
//                } catch (ParseException e) {
//                    logger.warning("Received illegal response :" + responseContent);
//
//                }
                singleClientSocket.close();


            } catch (SocketException e) {
                logger.warning("Failed to start SingleClient-" + currentThreadName + ": " + e.getMessage());
            }
        }

        public void handleResponse(JSONObject responseJson) {
            logger.info("[*] Handling response: " + responseJson.toJSONString());
        }
    }

}
