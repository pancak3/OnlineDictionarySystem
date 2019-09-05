import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner;
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
    private final static String UDP_SERVER_ADDR = "127.0.0.1";
    private final static int UDP_SERVER_PORT = 7397;

    private final static int REQUEST_SEND_CYCLE_MILLIS = 100;
    private final static int MAX_SEND_TIMES = 100;
    private final static int MAX_BUFFER_SIZE = 10240;
    private static boolean IS_DEBUG = false;


    private static AtomicReference<Integer> stressTestingSuccessNum = new AtomicReference<Integer>();
    private static AtomicReference<Integer> stressTestingFailedNum = new AtomicReference<Integer>();

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


        Scanner inputScanner = new Scanner(System.in);
        System.out.println("[*] Welcome to UDPClient.");


        System.out.println("    UDPClient terminal provides two modes -> \"Normal\" and \"Debug\".\r\n");
        System.out.println("    Normal: No debug information and details.");
        System.out.println("    Debug: With debug infos, advanced tools like Stress Testing.\r\n");
        System.out.print("Do you prefer \"Normal\"?(y/n): ");
        boolean inputIllegal = false;
        String input;
        for (int i = 0; i < 5; i++) {
            input = inputScanner.nextLine();
            if (input.equals("y") || input.equals("Y")) {
                inputIllegal = true;
                break;
            } else if (input.equals("n") || input.equals("N")) {
                IS_DEBUG = true;
                inputIllegal = true;
                break;
            }

        }
        if (inputIllegal) {
            String mode;
            if (IS_DEBUG) {
                mode = "(debug): ";
            } else {
                mode = "(normal): ";
            }
            String usage = "    Usage:\n          Simply input $commands, and then follow the instructions.\n          $commands include {Normal: .add .query .edit .remove .exit}";
            System.out.print(usage);
            if (IS_DEBUG) {
                System.out.println("+{Debug: .stressTest}");

            } else {
                System.out.println(" ");

            }
            System.out.print(mode);

            JSONObject temJson = new JSONObject();
            JSONObject userInputJson = new JSONObject();

//            while (true) {


            input = inputScanner.nextLine();
            boolean isAction = true;
            int runtimes = 1;
            switch (input) {
                case ".add":
                    System.out.print(mode + "wordName-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordName", input);

                    System.out.print(mode + "wordType-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordType", input);

                    System.out.print(mode + "wordMeaning-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordMeaning", input);

                    userInputJson.put("data", temJson);
                    userInputJson.put("action", "add");
                    break;

                case ".query":
                    System.out.print(mode + "wordName-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordName", input);

                    userInputJson.put("data", temJson);
                    userInputJson.put("action", "query");
                    break;

                case ".edit":

                    System.out.print(mode + "idx-> ");
                    input = inputScanner.nextLine();
                    temJson.put("idx", input);
                    System.out.print(mode + "wordName-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordName", input);

                    System.out.print(mode + "wordType-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordType", input);

                    System.out.print(mode + "wordMeaning-> ");
                    input = inputScanner.nextLine();
                    temJson.put("wordMeaning", input);

                    userInputJson.put("data", temJson);
                    userInputJson.put("action", "edit");
                    break;

                case ".remove":
                    System.out.print(mode + "idx-> ");
                    input = inputScanner.nextLine();

                    temJson.put("idx", input);
                    userInputJson.put("data", temJson);
                    userInputJson.put("action", "remove");
                    break;

                case ".exit":
                    isAction = false;

                    System.out.println("[*] Bye.");
                    System.exit(0);
                    break;

                case ".modeSwitch":
                    isAction = false;
                    IS_DEBUG = !IS_DEBUG;
                    break;

                case ".stressTest":
                    if (IS_DEBUG) {
                        System.out.print(mode + "How many clients do you want? -> ");
                        input = inputScanner.nextLine();


                        temJson.put("wordName", "TEST");
                        temJson.put("wordType", "TEST");
                        temJson.put("wordMeaning", "TEST");

                        userInputJson.put("data", temJson);
                        userInputJson.put("action", "add");

                        runtimes = Integer.parseInt(input);
                    } else {
                        System.out.println("[*] You are not in debug mode.");
                    }


                    break;

                default:
                    isAction = false;
                    System.out.println("[*] command not found.");

            }
            if (isAction) {
                stressTestingSuccessNum.set(0);
                stressTestingFailedNum.set(0);
                userInputJson.put("timestamp", System.currentTimeMillis());
                SingleClient singleClient = new SingleClient(userInputJson);
                for (int i = 0; i < runtimes; i++) {
                    new Thread(singleClient, "SingleClient-Main").start();

                }

            }
//                System.out.print(usage);
//                if (IS_DEBUG) {
//                    System.out.println(" {Debug: .stressTest}");
//
//                } else {
//                    System.out.println(" ");
//
//                }
//                if (IS_DEBUG) {
//                    mode = "(debug): ";
//                } else {
//                    mode = "(normal): ";
//                }
//                System.out.print(mode);
//            }

        } else {
            System.out.println("[*] Too many times try, bye.");
        }


//        stressTestingSuccessNum.set(0);
//        stressTestingFailedNum.set(0);
//        for (int i = 0; i < 500; ++i) {
//
//
//            //remove test
////        temJson.put("idx", "20");
////        userInputJson.put("data", temJson);
////        userInputJson.put("action", "remove");
//            userInputJson.put("timestamp", System.currentTimeMillis());
//
//            SingleClient singleClient = new SingleClient(userInputJson);
//            new Thread(singleClient, "SingleClient-" + i).start();
//        }
////        mainSingleClient.run();


    }


    private static class SingleClient implements Runnable {
        private final static String currentThreadName = Thread.currentThread().getName();
        private final static Logger logger = Logger.getLogger("SingleClient-" + currentThreadName);
        private final Action action = new Action();
        private final JSONParser parser = new JSONParser();
        private boolean wasRequestReceived = false;
        private boolean wasRequestSuccess = false;

        public void run() {
            if (!IS_DEBUG) {
                logger.setLevel(Level.SEVERE);
            }

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

                        byte[] buffer = new byte[MAX_BUFFER_SIZE];
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
                                            wasRequestSuccess = true;
                                            action.clearAction();
//                                            System.exit(0);
                                            break;
                                    }
                                }


                            } catch (ParseException e) {
                                logger.warning("Received illegal response :" + responseContent);

                            }

                        } catch (SocketTimeoutException ignored) {

                        }
                        action.updateAction(actionJson);


                    } catch (IOException e) {
                        logger.warning("Error while responding: " + e.getMessage());
                    }


                }
                if (!wasRequestSuccess) {
                    logger.info("[*] " + currentThreadName + " receive timeout ");
                    System.out.println("[*] Request timeout.");
                    stressTestingFailedNum.set(stressTestingFailedNum.get() + 1);
                } else {
                    stressTestingSuccessNum.set(stressTestingSuccessNum.get() + 1);
                }

                logger.info("[*] Success: " + stressTestingSuccessNum.get() + " Failed: " + stressTestingFailedNum.get()+" ("+(Integer.parseInt(String.valueOf(stressTestingFailedNum.get()))+Integer.parseInt(String.valueOf(stressTestingSuccessNum.get())))+" UDPClients were created.)");
                singleClientSocket.close();


            } catch (SocketException e) {
                logger.warning("Failed to start SingleClient-" + currentThreadName + ": " + e.getMessage());
            }
        }

        public void handleResponse(JSONObject responseJson) {
            logger.info("[*] SingleClient-" + currentThreadName + " is handling response: " + responseJson.toJSONString());

            if (!IS_DEBUG) {
                if (responseJson.containsKey("data")) {
                    if (responseJson.get("data").toString().length() > 2) {
                        System.out.println("[*] Success! Results are in Json format:");
                        System.out.println(responseJson.get("data").toString());
                    } else {
                        System.out.println("[*] Request successfully bu no result found.");
                    }
                }
            }
        }

        SingleClient(JSONObject actionJson) {
            this.action.updateAction(actionJson);
        }
    }

}
