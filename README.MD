## UDPServer and UDPClient
move to workplace and compile Server and Client
```aidl
    cd $PROJECT_PATH 
    ./build.sh
    cd bin/
```

Start UDPServer
```java
java -jar UDPServer.jar 

    [*] Welcome to UDPServer.
        UDPServer terminal provides two modes -> "Normal" and "Debug".
    
        Normal: Run with default config; Only warning logs.
        Debug: Set configs; Full logs.
    
    Do you prefer "Normal"?(y/n): y
    [*] Current config:
        Handlers Num: 200
        Confirmor Num: 5
        Responder Num: 5
    [*] UDP server started at port: 7397
```

Use UDPClient
```java
java -jar UDPClient.jar
    [*] Welcome to UDPClient.
        UDPClient terminal provides two modes -> "Normal" and "Debug".
    
        Normal: No debug information and details.
        Debug: With debug infos, advanced tools like Stress Testing.
    
    Do you prefer "Normal"?(y/n): y
        Usage:
              Simply input $commands, and then follow the instructions.
              $commands include {Normal: .add .query .edit .remove .exit} 
    (normal): 
```
 