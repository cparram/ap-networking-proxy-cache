package proxy;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cesar on 06-06-16.
 */
public class Daemon {

    @Option(name = "--port", usage = "listening port", required = true)
    private int port;
    @Option(name = "--path", usage = "Path to files", required = true)
    private String path;

    public static void main(String[] args) {
        Daemon server = new Daemon();

        if (server.config(args)) {
            server.listen();
        }
    }

    private void listen() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while(true) {
                Socket socket = serverSocket.accept();
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String clientMsg = getClientMsg(input);
                if (clientMsg.equals("LIST")) {
                    sendListFilesToClient(output);
                } else {
                    String action = clientMsg.split(" ")[0];
                    String fileName = clientMsg.split(" ")[1];
                    if (action.equals("GET")) {
                        if (fileExists(fileName))
                            sendFileToClient(fileName, output);
                        else {
                            // ToDo: get file from master
                            // and response to client
                            sendToClient("FNE", output);
                        }
                    }
                    // ToDo: implement other responses
                    // else if (action.equals("PUT")) {
                    //    if (!fileExists(fileName)) {
                    //        sendToClient("FNE", output);
                    //        receiveFile(fileName, input);
                    //        sendFileToMaster(fileName);
                    //    } else {
                    //        sendToClient("FYE", output);
                    //    }
                    // } else if (action.equals("DELETE")) {
                    //    if (fileExists(fileName))
                    //        removeMasterFile(fileName);
                    // }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void sendFileToClient(String fileName, DataOutputStream output) {
        FileInputStream serverFile;
        try {
            serverFile = new FileInputStream(path + fileName);
            byte[] buffer = new byte[1024];
            while (serverFile.read(buffer) != -1) {
                sendToClient(new String(buffer), output);
            }
            sendToClient("EOF", output);
            serverFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends to client a list of files
     * @param output
     */
    private void sendListFilesToClient(DataOutputStream output) {
        File folder = new File(path);
        for (File fileEntry : folder.listFiles()) {
            sendToClient(fileEntry.getName(), output);
        }
        sendToClient("OK", output);
    }

    /**
     * Checks if a file exists or not.
     * @param fileName
     * @return
     */
    private boolean fileExists(String fileName) {
        File file = new File(path + fileName);
        return file.exists();
    }

    /**
     * Sends messages to client.
     *
     * @param msg    Message that will be sent
     * @param output Data output stream
     */
    private void sendToClient(String msg, DataOutputStream output) {
        try {
            output.writeBytes(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets client message. It will wait until client sends a message
     *
     * @param input Reader
     * @return String message from client
     */
    private String getClientMsg(BufferedReader input) {
        String msg = "";
        try {
            msg = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    private boolean config(String[] args) {
        boolean configured = false;
        CmdLineParser parser = new CmdLineParser(this);
        try {
            // parse the arguments.
            parser.parseArgument(args);
            configured = true;
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
        }
        path = path.endsWith("/") ? path : path + "/";
        return configured;
    }
}
