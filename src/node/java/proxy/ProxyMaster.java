package proxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cesar on 07-06-16.
 */
public class ProxyMaster extends Thread {

    private final static int PORT=8181;
    private String path;
    private NodeUtil nodeUtil = new NodeUtil();

    public ProxyMaster(String path) {
        this.path = path;
    }


    @Override
    public void run() {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            while(true) {
                Socket socket = serverSocket.accept();
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String masterMsg = nodeUtil.receive(input);
                System.out.println("Message received from master: '" + masterMsg + "'");
                if (masterMsg.equals("SYNC")) {
                    System.out.println("Master wants sync");
                    nodeUtil.receiveListFile(path + ".config", input);
                    nodeUtil.send("OK", output);
                } else {
                    String action = masterMsg.split(" ")[0];
                    String fileName = masterMsg.split(" ")[1];
                    if (action.equals("DELETE")) {
                        if (nodeUtil.fileExists(path + fileName)) {
                            if (nodeUtil.deleteFile(path + fileName)) {
                                nodeUtil.send("OK", output);
                            } else {
                                nodeUtil.send("FAIL", output);
                            }
                        } else {
                            nodeUtil.send("FNE", output);
                        }
                    } else
                        System.out.println("Invalid message: '" + masterMsg + "' received from master");
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
