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

    private final static int REQUEST_SEND_CYCLE_MILLIS = 100;
    private final static int MAX_SEND_TIMES = 30;

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
        mainSingleClient.action.updateAction(userInputJson);
        mainSingleClient.run();

    }


    private static class SingleClient implements Runnable {
        private final static String currentThreadName = Thread.currentThread().getName();
        private final static Logger logger = Logger.getLogger("SingleClient-" + currentThreadName);
        private final Action action = new Action();
        private final JSONParser parser = new JSONParser();

        public void run() {
            logger.info("[*] Running SingleClient-" + currentThreadName);
            try {
                DatagramSocket singleClientSocket = new DatagramSocket();
                JSONObject actionJson = this.action.getAction();
                int actionRequestTimes = Integer.parseInt(actionJson.get("requestTimes").toString());

                while (actionRequestTimes <= MAX_SEND_TIMES) {

                    actionJson = this.action.getAction();
                    actionRequestTimes = Integer.parseInt(actionJson.get("requestTimes").toString());
                    long actionTimestamp = Long.parseLong(actionJson.get("timestamp").toString());
                    long nowTimestamp = System.currentTimeMillis();
                    if (actionJson.containsKey("action") && nowTimestamp - actionTimestamp > REQUEST_SEND_CYCLE_MILLIS) {

                        try {
                            JSONObject requestJson = (JSONObject) new JSONObject();

                            requestJson.put("action", actionJson.get("action").toString());
                            requestJson.put("data", actionJson.get("data").toString());

                            String requestString = requestJson.toJSONString();
                            byte[] requestBytes = requestString.getBytes();
                            DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(UDP_SERVER_ADDR), UDP_SERVER_PORT);
                            singleClientSocket.send(requestPacket);
//                            logger.info("[*] Sent :" + requestString + " with hashCode:" + requestString.hashCode());
                            //set receive time out
                            singleClientSocket.setSoTimeout(REQUEST_SEND_CYCLE_MILLIS);
                            byte[] buffer = new byte[1024];
                            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);


                            try {
                                singleClientSocket.receive(responsePacket);
                                String responseContent = new String(responsePacket.getData()).substring(0, requestPacket.getLength());
                                System.out.println("[-] Got response from server: " + responseContent);

                                try {
                                    JSONObject responseJson = (JSONObject) parser.parse(responseContent);
                                    String requestStatus = (String) responseJson.get("status").toString();
                                    switch (requestStatus) {
                                        case "received":
                                            logger.info("[*] request arrived.");
                                            break;
                                        case "success":
                                            logger.info("[*] request success.");
                                            actionJson.put("requestTimes", MAX_SEND_TIMES + 1);
                                            break;
                                    }
                                } catch (ParseException e) {
                                    logger.warning("Received illegal response :" + responseContent);

                                }

                            } catch (SocketTimeoutException e) {
                                logger.info("[*] " + currentThreadName + " receive timeout: " + e.getMessage());
                            }

                            logger.info("[*] tried " + actionRequestTimes + " times.");

                            action.updateAction(actionJson);

                        } catch (IOException e) {
                            logger.warning("Error while responding: " + e.getMessage());
                        }

                    }
                }
                singleClientSocket.close();


            } catch (SocketException e) {
                logger.warning("Failed to start SingleClient-" + currentThreadName + ": " + e.getMessage());
            }
        }

    }

}
