import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Correct usage: java ChatClient <ChatServerHostName> <portNumber>");
            System.exit(0);
        }
        String hName = args[0];
        int pNum = Integer.parseInt(args[1]);

        try {
            Socket cliSock = new Socket(hName, pNum);
            Input inp = new Input(cliSock);
            Recv r = new Recv(cliSock);
            inp.start();
            r.start();
        } catch (Exception e){
            System.err.println("Failed to initialize threads.");
            System.exit(0);
        }
    }
}

class Input extends Thread {
    // put streams here
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
            io = new BufferedReader(new InputStreamReader(System.in));
            pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);

            while (!Recv.nameFlag) {
                name = io.readLine();
                pw.println("REG " + name);
                synchronized(this) {
                    this.wait(500);
                }
            }
            while (true) {
                String[] pcs = io.readLine().split(" ");
                if (pcs[0].equals("LEAVE")) {
                    pw.println("EXIT " + this.name);
                } else if (pcs[0].equals("@")) {
                    String[] txt = new String[pcs.length-2];
                    for (int i = 2; i < pcs.length; i++) { txt[i-2] = pcs[i]; }
                    pw.println("PMSG " + pcs[1] + " " + String.join("\t", txt));
                } else {
                    pw.println("MESG " + String.join("\t", pcs));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read input.");
            System.exit(0);
        } catch (InterruptedException e) {
            System.err.println("Thread messed up.");
            System.exit(0);
        }
    }
}

class Recv extends Thread {
    // put streams here
    Socket s;
    BufferedReader br;
    public static boolean nameFlag;

    public Recv(Socket cs) {
        s = cs;
        nameFlag = false;
    }

    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            System.out.println("Enter your username (no spaces, 1-32 characters long): ");
            while (true) {
                String[] servMsg = br.readLine().split(" ");
                if (!nameFlag) {
                    if (servMsg[0].equals("ACK")) {
                        nameFlag = true;
                        System.out.println("There are " + servMsg[1] + " users in the chatroom.");
                        System.out.println("..........................................................................................");
                        for (String u: servMsg[2].split("\\t")) { System.out.println(u); }
                        System.out.println("..........................................................................................");
                        System.out.println("Chatroom Rules");
                        System.out.println("1) To type to the whole room, simply type your message.");
                        System.out.println("2) To type a private message to a user with name <x>, use the format \"@ <x> <message>\".");
                        System.out.println("3) To leave the room, simply type LEAVE in all caps.");
                        System.out.println("..........................................................................................");
                    } else if (servMsg[0].equals("ERR")){
                        switch (Integer.parseInt(servMsg[1])) {
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
                        System.out.println("Enter your username: ");
                    }
                } else {
                    if (servMsg[0].equals("ERR")) {
                        if (servMsg[1].equals("3")) {
                            System.out.println("The user you tried to privately message doesn't exist.");
                        } else {
                            System.out.println("Unknown message format.");
                        }
                    } else if (servMsg[0].equals("ACK")) {
                        System.out.println("You left the chatroom. There are " + servMsg[1] + " users in the chatroom.");
                        System.out.println("..........................................................................................");
                        if (servMsg.length == 3) { for (String u: servMsg[2].split("\\t")) { System.out.println(u); } }
                        System.out.println("..........................................................................................");
                        System.out.println("Goodbye!");
                        System.exit(0);
                    } else if (servMsg[0].equals("MSG")) {
                        if (servMsg[1].equals("SERVER")) {
                            if (servMsg[2].equals("L")) {
                                System.out.println("SERVER NOTICE: " + servMsg[3] + " left the room.");
                            } else if (servMsg[2].equals("J")) {
                                System.out.println("SERVER NOTICE: " + servMsg[3] + " joined the room.");
                            }
                        } else {
                            System.out.println(servMsg[1] + ": " + String.join(" ", servMsg[2].split("\\t")));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read server message.");
            System.exit(0);
        }
    }
}