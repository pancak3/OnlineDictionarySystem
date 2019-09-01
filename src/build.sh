echo -e "[*] Building Database ... \c"
#compile Database
DATABASE_PATH="./Database/"
javac $DATABASE_PATH/Database.java
jar cfm $DATABASE_PATH/Database.jar $DATABASE_PATH/MANIFEST.MF $DATABASE_PATH/Database.class
cp $DATABASE_PATH/Database.jar ../lib/
echo -e "done."

echo -e "[*] Building UDPServer ... \c"
#compile UDPServer
UDPSERVER_PATH="./"
javac $UDPSERVER_PATH/UDPServer.java
jar cfm $UDPSERVER_PATH/UDPServer.jar $UDPSERVER_PATH/MANIFEST-UDP_SERVER.MF $UDPSERVER_PATH/UDPServer.class
echo -e "done."

echo -e "[*] Building UDPClient ... \c"
#compile UDPClient
UDPCLIENT_PATH="./"
javac $UDPCLIENT_PATH/UDPClient.java
jar cfm $UDPCLIENT_PATH/UDPClient.jar $UDPCLIENT_PATH/MANIFEST-UDP_CLIENT.MF $UDPCLIENT_PATH/UDPClient.class
echo -e "done."