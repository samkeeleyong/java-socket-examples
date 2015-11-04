package pm.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Application {

    private  final static int PORT = 9999;
    private final static HashSet<String> NAMES = new HashSet<String>();
    private static HashSet<Handler> HANDLERS = new HashSet<>(); 
    private static final StringBuilder history = new StringBuilder();
    private static Map<String, Set<PrintWriter>> ROOMS = new HashMap<>();
    
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        
        try (ServerSocket listener = new ServerSocket(PORT);) {
            while (true) {
                Handler h = new Handler(listener.accept());
                HANDLERS.add(h);
                h.start();
            }
        }
    }
    
    private static class Handler extends Thread {
        String name1;
        String name2;
        String keyName1;
        String keyName2;
        Socket socket;
        BufferedReader in;   // to receive messages
        PrintWriter out;     // to send messages
        HashSet<PrintWriter> WRITERS = new HashSet<>();
        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // ask for a unique client handle
                while (true) {
                    out.println("SUBMITNAME1");
                    name1 = in.readLine();
                    if (name1 == null) {
                        return;
                    }
                    synchronized (NAMES) {
                        if (!NAMES.contains(name1)) {
                            NAMES.add(name1);
                            out.println("NAMEACCEPTED");
                            System.out.println(name1 + " has connected.");
                            break;
                        }
                    }
                }
                
                out.println("SUBMITNAME2");
                name2 = in.readLine();
                System.out.println(name1 + " wants to chat with " + name2);
                
                keyName1= name2 + "-" + name1;
                keyName2 = name1 + "-" + name2;
                if(!ROOMS.containsKey(keyName1)){
                	WRITERS.add(out);
                	ROOMS.put(name1 + "-" + name2, WRITERS);
                } else{
                	ROOMS.get(keyName1).add(out);
                }
//                WRITERS.add(out);   // add to the list of to-broadcast clients
                
                broadCastHistory(history.toString(), out);
                broadCastMessage("MESSAGE:" + name1 + " just joined the chatroom.");
                
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    broadCastMessage("MESSAGE " + name1 + ": " + input);
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name1 != null) {
                    NAMES.remove(name1);
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
			Set<PrintWriter> set = null;
			if(ROOMS.get(keyName1) == null){
				set = ROOMS.get(keyName2); 
			} else{
				set = ROOMS.get(keyName1);
			}
			
            for (PrintWriter writer : set) {
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
