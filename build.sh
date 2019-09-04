BIN_PATH="./bin"

echo -e "[*] Compiling Database ... \c"
#compile Database
DATABASE_PATH="./UDPServer/Database"
javac -cp \
  ./bin/sqlite-jdbc-3.27.2.1.jar:./bin/json-simple-1.1.1.jar \
  $DATABASE_PATH/Database.java
echo "done."

#compile UDPServer
echo -e "[*] Compiling UDPServer ... \c"
UDPSERVER_PATH="./UDPServer"
javac -cp \
  ./bin/sqlite-jdbc-3.27.2.1.jar:./bin/json-simple-1.1.1.jar:./$UDPSERVER_PATH \
  $UDPSERVER_PATH/UDPServer.java

jar --create \
  --file $BIN_PATH/UDPServer.jar \
  --manifest $UDPSERVER_PATH/MANIFEST.MF \
  $UDPSERVER_PATH/Database/Database.class \
  $UDPSERVER_PATH/Database/User.class \
  $UDPSERVER_PATH/Database/Word.class \
  -C $UDPSERVER_PATH UDPServer.class \
  -C $UDPSERVER_PATH UDPServer\$Confirmor.class \
  -C $UDPSERVER_PATH UDPServer\$Handler.class \
  -C $UDPSERVER_PATH UDPServer\$Receiver.class \
  -C $UDPSERVER_PATH UDPServer\$Responder.class \
  -C $UDPSERVER_PATH UDPServer\$ResponseTask.class

echo -e "done."

#compile UDPClient
echo -e "[*] Compiling UDPClient ... \c"
UDPCLIENT_PATH="./UDPClient"
javac -cp \
  ./bin/json-simple-1.1.1.jar:./$UDPCLIENT_PATH \
  $UDPCLIENT_PATH/UDPClient.java
jar --create \
  --file $BIN_PATH/UDPClient.jar \
  --manifest $UDPCLIENT_PATH/MANIFEST.MF \
  -C $UDPCLIENT_PATH UDPClient.class \
  -C $UDPCLIENT_PATH UDPClient\$User.class \
  -C $UDPCLIENT_PATH UDPClient\$Sender.class \
  -C $UDPCLIENT_PATH UDPClient\$Action.class
echo -e "done."

echo -e "\r\n[-] Note: Due to the use of Sqlite file, should always run UDPServer or UDPServer under \$Project_Path/bin/"
