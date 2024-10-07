# ChatApp
### Setting up ChatApp
1. Take the `ChatClient.java` and `ChatServer.java` and upload them to a new directory, D, on any UTD net01 - net45 machine.
2. Navigate to D, type `javac *.java` and hit Enter to compile the two Java files.
3. Type `ls` and verify that along with the Java files, you have five class files: ChatClient, ChatServer, Input, Recv, and Handler.

### Running ChatApp
1. After setting up ChatApp, on any machine netYY, type `java ChatServer XXXX`, where XXXX is your desired port number and YY is your initial machine.
2. On any other machine, type `java ChatClient netYY.utdallas.edu XXXX` to start up the client. You should see a username prompt.
3. Try to break the conditions of the username, and verify that the proper registration fail reason pops up each time before the reprompt appears.
4. On successful join, verify that the room participants count, room participants list, and chat rules show up.
5. Try broadcast messaging, following the rules in the next section, and verify that everyone can see the message except the user themselves.
6. Try private messaging, following the rules in the next section, and verify that the intended recipient sees the message, but no one else can.
7. Try leaving, following the rules in the next section, and verify that the server continues to run, and the standard leave message is displayed to others.
8. Also try exiting the program unexpectedly (for example, hitting `Ctrl + C`), and verify that the server continues to run, and the standard leave message is displayed to others.


### Input-to-Client Message Translation
1. Name registration is just a prompt, and is just prepended by `'REG '` in the Input class.
2. Leaving is done by typing LEAVE in all caps. Input class simply sends `'EXIT ' + name` to the server.
3. Private messaging is done via the @ symbol (similar to Discord), separated by a space, the recipient, a space, and the rest of the message.
4. Broadcast messaging is any other input: simply type, hit Enter, and it is available for everyone to see.
   
### Server-to-Output Message Translation
1. Registration ACK lets the user know how many users are in the room, their names, and the rules of the chat room (basically the above section, but formatted nicer).
2. Registration failures let the user know which condition (no spaces, 1-32 char. long, taken name) they failed on, and reprompts them.
3. If `ERR 3` is received by the client-side, the client-side lets the user know that the recipient does not exist.
4. If an ACK besides registration ACK is sent, it is because the user is leaving. It shows the new list of room participants WITHOUT them included and then says a goodbye message.
5. Any other error received by the client-side will be `ERR 4`.


