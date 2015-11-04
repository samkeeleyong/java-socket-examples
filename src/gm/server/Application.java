package gm.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class Application {

    private  final static int PORT = 9999;
    private final static HashSet<String> NAMES = new HashSet<String>();
    private static HashSet<PrintWriter> WRITERS = new HashSet<PrintWriter>();
    private static final StringBuilder history = new StringBuilder();
    
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        
        try (ServerSocket listener = new ServerSocket(PORT);) {
            while (true) {
                new Handler(listener.accept()).start();
            }
        }
    }
    
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;   // to receive messages
        private PrintWriter out;     // to send messages

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // ask for a unique client handle
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (NAMES) {
                        if (!NAMES.contains(name)) {
                            NAMES.add(name);
                            out.println("NAMEACCEPTED");
                            System.out.println(name + " has connected.");
                            break;
                        }
                    }
                }
                
                WRITERS.add(out);   // add to the list of to-broadcast clients
                
                broadCastHistory(history.toString(), out);
                broadCastMessage("MESSAGE:" + name + " just joined the chatroom.");
                
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    broadCastMessage("MESSAGE " + name + ": " + input);
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    NAMES.remove(name);
                }
                if (out != null) {
                	WRITERS.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

		private void broadCastMessage(String input) {
			history.append(input + "\n");
            for (PrintWriter writer : WRITERS) {
                writer.println(input);
            }
		}
		
		private void broadCastHistory(String input, PrintWriter out) {
			if(NAMES.size() == 1){
				out.println("MESSAGE:Start of conversation");
				return; 
			}
			out.println(input);
		}
    }
}
