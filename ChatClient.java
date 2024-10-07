import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) { // program stops if wrong usage
            System.err.println("Correct usage: java ChatClient <ChatServerHostName> <portNumber>");
            System.exit(0);
        }
        String hName = args[0]; // hostName
        int pNum = Integer.parseInt(args[1]); // portNumber

        try {
            Socket cliSock = new Socket(hName, pNum);
            Input inp = new Input(cliSock); // create a new standard IO handler
            Recv r = new Recv(cliSock); // create a new server-side handler
            inp.start();
            r.start();
        } catch (Exception e){ // if something fails...
            System.err.println("Failed to initialize client.");
            System.exit(0);
        }
    }
}

class Input extends Thread { // Handles all input from user console and prepares it to send to the server
    Socket s;
    BufferedReader io;
    PrintWriter pw;
    String name; 
    public Input(Socket cs) {
        s = cs;
        this.name = null;
    }

    @Override
    public void run() {
        try {
            io = new BufferedReader(new InputStreamReader(System.in)); // new console reader
            pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true); // new writer to server-side

            while (!Recv.nameFlag) {
                name = io.readLine(); // keeps track of current name (since no way for server to communicate which name actually registered and worked)
                pw.println("REG " + name); // attempt registration
                synchronized(this) {
                    this.wait(500); // wait for Recv class to update nameFlag
                }
            }
            while (true) {
                String[] pcs = io.readLine().split(" "); // parsing step
                if (pcs[0].equals("LEAVE")) { // if it's LEAVE
                    pw.println("EXIT " + this.name); // send EXIT
                } else if (pcs[0].equals("@")) { // if it begins with an @, it's a private message
                    String[] txt = new String[pcs.length-2]; // the actual message begins at pcs[2] onwards
                    for (int i = 2; i < pcs.length; i++) { txt[i-2] = pcs[i]; } // copy it over
                    pw.println("PMSG " + pcs[1] + " " + String.join("\t", txt)); // send PMSG
                } else { // regular old broadcast 
                    pw.println("MESG " + String.join("\t", pcs)); // send MESG
                }
            }
        } catch (IOException e) { // if stream failed somehow
            System.err.println("Failed to read input.");
            System.exit(0);
        } catch (InterruptedException e) { // if server closed unexpectedly
            System.err.println("Thread messed up.");
            System.exit(0);
        }
    }
}

class Recv extends Thread { // Handles all messages incoming from server-side, and formats the outputs for the user.
    Socket s;
    BufferedReader br;
    public static boolean nameFlag;

    public Recv(Socket cs) {
        s = cs;
        nameFlag = false; // initially, we have no name for this client
    }

    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(s.getInputStream())); // new reader from server-side
            System.out.println("Enter your username (no spaces, 1-32 characters long): "); // initially username query
            while (true) {
                String[] servMsg = br.readLine().split(" ");
                if (!nameFlag) { // if we don't have a name yet, that's all we will ask for, as we want to lock other functionality until the user is registered
                    if (servMsg[0].equals("ACK")) { // if we get a successful registration
                        nameFlag = true; // put up nameFlag for Input class
                        // standard introductory message
                        System.out.println("There are " + servMsg[1] + " users in the chatroom.");
                        System.out.println("..........................................................................................");
                        for (String u: servMsg[2].split("\\t")) { System.out.println(u); }
                        System.out.println("..........................................................................................");
                        System.out.println("Chatroom Rules");
                        System.out.println("1) To type to the whole room, simply type your message.");
                        System.out.println("2) To type a private message to a user with name <x>, use the format \"@ <x> <message>\".");
                        System.out.println("3) To leave the room, simply type LEAVE in all caps.");
                        System.out.println("..........................................................................................");
                    } else if (servMsg[0].equals("ERR")){ // bad registration, lets the user know what went wrong
                        switch (Integer.parseInt(servMsg[1])) { // servMsg[1] is the error code
                            case 0:
                                System.out.println("You chose a taken name.");
                                break;
                            case 1:
                                System.out.println("Your name must be at most 32 characters long.");
                                break;
                            case 2:
                                System.out.println("Your name has spaces.");
                                break;
                            default:
                                System.out.println("Unknown registration message format.");
                        }
                        System.out.println("Enter your username: "); // since failed registration, reprompt the user for a username
                    }
                } else {
                    if (servMsg[0].equals("ERR")) { // errors once the user has registered
                        if (servMsg[1].equals("3")) { // code 3 is for nonexistent recipient
                            System.out.println("The user you tried to privately message doesn't exist.");
                        } else { // simply just a bad message
                            System.out.println("Unknown message format.");
                        }
                    } else if (servMsg[0].equals("ACK")) { // only ACK one can receive after registering successfully is if you leave
                        System.out.println("You left the chatroom. There are " + servMsg[1] + " users in the chatroom.");
                        System.out.println("..........................................................................................");
                        if (servMsg.length == 3) { for (String u: servMsg[2].split("\\t")) { System.out.println(u); } }
                        System.out.println("..........................................................................................");
                        System.out.println("Goodbye!");
                        System.exit(0);
                    } else if (servMsg[0].equals("MSG")) { // when there is an incoming message
                        if (servMsg[1].equals("SERVER")) { // if it is a server notice
                            if (servMsg[2].equals("L")) { // someone left
                                System.out.println("SERVER NOTICE: " + servMsg[3] + " left the room.");
                            } else if (servMsg[2].equals("J")) { // someone joined
                                System.out.println("SERVER NOTICE: " + servMsg[3] + " joined the room.");
                            }
                        } else { // just print out the message
                            System.out.println(servMsg[1] + ": " + String.join(" ", servMsg[2].split("\\t")));
                        }
                    }
                }
            }
        } catch (IOException e) { // if something went wrong with reading from the server-side
            System.err.println("Failed to read server message.");
            System.exit(0);
        }
    }
}
