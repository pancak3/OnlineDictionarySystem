import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.*;

import java.util.logging.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.print.DocFlavor;

//to remove warning both in IJ and compile, but guess there is better way to use Json
@SuppressWarnings("unchecked")

public class UDPClient {
    private final static String UDP_SERVER_ADDR = "localhost";
    private final static int UDP_SERVER_PORT = 7397;

    private final static int REQUEST_SEND_CYCLE_MILLIS = 100;
    private final static int MAX_SEND_TIMES = 1;

    private final static JSONObject requestJson = (JSONObject) new JSONObject();
    private final static Action action = (Action) new Action();

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

        // start user
        new Thread(new User(), "User").start();
        // start Sender
        new Thread(new Sender(), "Sender").start();

        try {

//            System.out.println("This is UDP Client- Enter some text to send to the UDP server");
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // Create a UDP socket object
            clientSocket = new DatagramSocket();
            //IP and port for socket (static method)

            // As UDP Datagrams are bounded by fixed message boundaries, define the length
            byte[] requestBytes;
            byte[] responseBytes = new byte[1024];

            String request = userInput.readLine();
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

    static class User implements Runnable {
        private final static Logger logger = Logger.getLogger("User");

        public void run() {
            logger.info("Starting user ...");
            JSONObject temJson = new JSONObject();
            JSONObject userInputJson = new JSONObject();
            temJson.put("wordName", "banana");
            userInputJson.put("data", temJson);
            userInputJson.put("action", "query");
            action.updateAction(userInputJson);

        }
    }

    static class Sender implements Runnable {
        private final static Logger logger = Logger.getLogger("Sender");

        public void run() {
            try {


                while (true) {
                    DatagramSocket responderSocket = new DatagramSocket();
                    JSONObject actionJson = action.getAction();
                    int actionRequestTimes = Integer.parseInt(actionJson.get("requestTimes").toString());
                    long actionTimestamp = Long.parseLong(actionJson.get("timestamp").toString());

                    long nowTimestamp = System.currentTimeMillis();

                    if (actionJson.containsKey("action") && nowTimestamp - actionTimestamp > REQUEST_SEND_CYCLE_MILLIS) {
                        if (actionRequestTimes >= MAX_SEND_TIMES) {
                            logger.warning("Action request sent times exceeded " + MAX_SEND_TIMES + " ,now give it up: " + actionJson.toJSONString());
                            action.clearAction();
                        } else {
                            try {
                                JSONObject requestJson = (JSONObject) new JSONObject();

                                requestJson.put("action", actionJson.get("action").toString());
                                requestJson.put("data", actionJson.get("data").toString());
                                requestJson.put("requestHashCode", actionJson.hashCode());
                                String requestString = requestJson.toJSONString();
                                byte[] requestBytes = requestString.getBytes();
                                DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(UDP_SERVER_ADDR), UDP_SERVER_PORT);
                                responderSocket.send(requestPacket);
                                action.updateAction(actionJson);

                            } catch (IOException e) {
                                logger.warning("Error while responding: " + e.getMessage());
                            }
                        }
                    }
                    responderSocket.close();
                }

            } catch (SocketException e) {
                logger.warning("Failed to start Sender: " + e.getMessage());
                System.exit(0);
            }
        }
    }

}
