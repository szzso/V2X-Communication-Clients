package communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import communication.Message.MessageCommon;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;

public class SocketServer extends Thread {

    private boolean serverOn;
    private ServerSocket serverSocket;
    private int port;
    private Queue<String> queue;

    public SocketServer(Queue<String> queue, int port) {
        this.queue = queue;
        this.port = port;
        serverOn = true;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Read message");
                    readMessage(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException ioe) {
            System.out.println("Could not create server socket on port " + port + ". Quitting.");
        } finally {
            // Clean up
            try {
                serverSocket.close();
                System.out.println("...Stopped");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void readMessage(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            // At this point, we can read for input and reply with appropriate output.

            // Run in a loop until m_bRunThread is set to false
            serverOn = true;
            while (serverOn && socket.isConnected()) {
                // read incoming stream
                String messageJSON = in.readLine();

                CommandEnum command;
                if (messageJSON != null && messageJSON.length() > 0) {
                    System.out.println("Client Says :" + messageJSON);
                    command = FactoryMessage.getCommandType(messageJSON);
                    if (command == CommandEnum.QUIT) {
                        // Special communication.command. Quit this thread
                        serverOn = false;
                        System.out.print("Stopping client thread for client ");
                    }
                    if (command == CommandEnum.ROUTE || command == CommandEnum.REROUTE) {
                        queue.offer(messageJSON);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*private String parseMessage(M message) {
        ObjectMapper objectMapper = new ObjectMapper();
        String messageJSON = "";
        try {
            messageJSON = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return messageJSON;
    }*/
}
