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
  ./lib/sqlite-jdbc-3.27.2.1.jar:./lib/json-simple-1.1.1.jar:./$UDPSERVER_PATH \
  $UDPSERVER_PATH/UDPServer.java
jar --create \
  --file $BIN_PATH/UDPServer.jar \
  --main-class UDPServer \
  -C $UDPSERVER_PATH .
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
  -C $UDPSERVER_PATH .
echo -e "done."
