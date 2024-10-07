import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    public static ConcurrentHashMap<String, PrintWriter> writerName = new ConcurrentHashMap<>(); // maps usernames to their PrintWriters for efficiency
    public static void main(String[] args) throws IOException {
        if (args.length != 1) { // program stops if server not initiated correctly
            System.err.println("Correct usage: java ChatServer <portNumber>");
            System.exit(0);
        }
        int pNum = Integer.parseInt(args[0]); // take port number as an arg
        System.out.println("> Chat room hosted on " + InetAddress.getLocalHost().getHostName() + ", using port " + pNum + ", online."); // online msg
        try {
            ServerSocket servSock = new ServerSocket(pNum); // new server socket
            while (true) { // accept connections as long as they come
                Socket cliSock = servSock.accept(); // accept the new client
                System.out.println("> Client from " + cliSock.getInetAddress().getHostName() + " connected."); // socket message
                Handler h = new Handler(cliSock); // new handler for THIS client
                h.start();
            }
        } catch (IOException e) {
            System.err.println("> Couldn't start the server."); // unknown exception catch
            System.exit(0);
        }
    }
}

class Handler extends Thread {
    private Socket s;
    private PrintWriter pw;
    private BufferedReader br;
    private String name;

    public Handler(Socket q) { s = q; } // this socket for this handler object

    public void run() {
        try {
            pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true); // create an auto-flushing writer to the client-side
            br = new BufferedReader(new InputStreamReader(s.getInputStream())); // create a reader from the client-side

            while (true) { // continually listen for input
                String inp = br.readLine(); // input
                if (inp == null) { // in case of a disconnect
                    cleanup();
                    break;
                }
                System.out.println("> Received message from " + s.getInetAddress().getHostName() + ": " + inp); // server message receiving input
                String[] msg = inp.split(" "); // parsing step
                if (msg[0].equals("REG")) { // registration msg
                    if (msg.length != 2) { // spaces
                        pw.println("ERR 2");
                        System.out.println("> Unnamed user on " + s.getInetAddress().getHostName() + ": spaces in name.");
                    } else if (ChatServer.writerName.containsKey(msg[1])) { // taken name
                        pw.println("ERR 0");
                        System.out.println("> Unnamed user on " + s.getInetAddress().getHostName() + ": taken name.");
                    } else if (msg[1].length() > 32) { // name too long
                        pw.println("ERR 1");
                        System.out.println("> Unnamed user on " + s.getInetAddress().getHostName() + ": name too long.");
                    } else { // good name
                        name = msg[1];
                        for (String n : ChatServer.writerName.keySet()) { ChatServer.writerName.get(n).println("MSG SERVER J " + name); } // let everyone know
                        ChatServer.writerName.put(name, pw); // store, for future use
                        pw.println("ACK " + ChatServer.writerName.size() + " " + String.join("\t", ChatServer.writerName.keySet())); // ACK msg
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + ": registration successful.");
                    }
                } else if (msg[0].equals("MESG")) { // msg to everyone
                    if (name == null) { // if no name yet, you are not registered, and thus cannot message anyone
                        pw.println("ERR 4");
                        System.out.println("> Unnamed user on " + s.getInetAddress().getHostName() + ": not registered.");
                    } else if (msg.length != 2) { // unknown format
                        pw.println("ERR 4");
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + ": bad group message format.");
                    } else { // good msg
                        for (String n : ChatServer.writerName.keySet()) { // for every user
                            if (!n.equals(name)) { // except yourself
                                ChatServer.writerName.get(n).println("MSG " + name + " " + msg[1]); //incoming message will be tabSep
                            }
                        }
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + ": sent a message to everyone else.");
                    }
                } else if (msg[0].equals("PMSG")) { // private msg
                    if (name == null) { // if no name yet, you are not registered, and thus cannot message anyone
                        pw.println("ERR 4");
                        System.out.println("> Unnamed user on " + s.getInetAddress().getHostName() + ": not registered.");
                    } else if (msg.length != 3) { // unknown format
                        pw.println("ERR 4");
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + " bad private message format.");
                    } else if (!ChatServer.writerName.keySet().contains(msg[1])) { // unknown recipient
                        pw.println("ERR 3");
                    } else { // good PM
                        ChatServer.writerName.get(msg[1]).println("MSG " + name + " " + msg[2]);
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + ": sent a message to " + msg[1] + ".");
                    }
                } else if (msg[0].equals("EXIT")) { // de-registration msg
                    if (name == null) { // if no name yet, you are not registered, and thus cannot message anyone
                        pw.println("ERR 4");
                        System.out.println("> Unnamed user on " + s.getInetAddress().getHostName() + ": not registered.");
                    } else if (msg.length != 2) { // unknown format
                        pw.println("ERR 4");
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + ": bad exit format.");
                    } else if (!msg[1].equals(name)) { // can't de-register anyone but yourself
                        pw.println("ERR 4");
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + ": only de-register self.");
                    } else { // good exit
                        ChatServer.writerName.remove(name); // remove from map (no more broadcasting to you! or PMs)
                        for (String n : ChatServer.writerName.keySet()) { // for all users
                            ChatServer.writerName.get(n).println("MSG SERVER L " + name);
                        }
                        pw.println("ACK " + ChatServer.writerName.size() + " " + String.join("\t", ChatServer.writerName.keySet())); // updated ACK
                        System.out.println("> " + name + " on " + s.getInetAddress().getHostName() + " exited.");
                        pw.close(); // terminate socket connection
                        br.close();
                        s.close();
                        break;
                    }
                } else { // any other case falls through to here
                    pw.println("ERR 4");
                }
            }
        } catch (IOException e) {
            System.err.println("> Client on " + s.getInetAddress().getHostName() + " mishandled.");
            cleanup();
        }
    }
    private void cleanup() { // in case of socket connection closing
        // no need to close the PW and BR, as they close upon socket close
        if (name != null && ChatServer.writerName.containsKey(name)) { // if there was a registered user and their name still exists in the list of users
            ChatServer.writerName.remove(name);
            for (String n : ChatServer.writerName.keySet()) { // Notify others
                ChatServer.writerName.get(n).println("MSG SERVER L " + name);
            }
            System.out.println("> Client " + name + " disconnected unexpectedly.");
        }
    }
}
