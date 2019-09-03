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
  -C $UDPSERVER_PATH UDPServer\$confirmor.class \
  -C $UDPSERVER_PATH UDPServer\$handler.class \
  -C $UDPSERVER_PATH UDPServer\$receiver.class \
  -C $UDPSERVER_PATH UDPServer\$responder.class \
  -C $UDPSERVER_PATH UDPServer\$ResponseTask.class \

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

echo "[-] Note: Due to use sqlite file as database, should always run UDPServer under bin/ path"