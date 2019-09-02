BIN_PATH="./bin"

echo -e "[*] Compiling Database ... \c"
#compile Database
DATABASE_PATH="./UDPServer/Database"
javac -cp ./lib/sqlite-jdbc-3.27.2.1.jar $DATABASE_PATH/Database.java
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
  -C $UDPSERVER_PATH UDPServer.class


echo -e "done."

#compile UDPClient
echo -e "[*] Compiling UDPClient ... \c"
UDPSERVER_PATH="./UDPClient"
javac -cp \
  ./lib/json-simple-1.1.1.jar:./$UDPSERVER_PATH \
  $UDPSERVER_PATH/UDPClient.java
jar --create \
  --file $BIN_PATH/UDPClient.jar \
  --main-class UDPClient \
  -C $UDPSERVER_PATH UDPClient.class
echo -e "done."
