package proxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cesar on 07-06-16.
 */
public class ClientRequest extends Thread {
    private final String path;
    private final int port;

    public ClientRequest(int port, String path) {
        this.port = port;
        this.path = path;
    }

    @Override
    public void run() {
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
        File file = new File(path + ".config/master_files.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sendToClient(line, output);
            }
            sendToClient("OK", output);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
}
