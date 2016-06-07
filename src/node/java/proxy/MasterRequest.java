package proxy;

import java.io.*;
import java.net.Socket;

/**
 * Created by cesar on 07-06-16.
 */
public class MasterRequest extends Thread {

    private final int portMaster;
    private final String ipMaster;
    private final String path;
    private final int porProxyMaster;

    public MasterRequest(int portMaster, String ipMaster, String path, int portProxyMaster) {
        this.portMaster = portMaster;
        this.ipMaster = ipMaster;
        this.path = path;
        this.porProxyMaster = portProxyMaster;
    }

    @Override
    public void run() {
        // the proxy will synchronize with master and then will open a port to listen master.
        try {
            Socket socket = new Socket(ipMaster, portMaster);
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            sendToMaster("SINCRONIZE", outStream);
            receiveListFile(inStream);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveListFile(BufferedReader inStream) {
        // Creation of config dir
        File proxyConfig = new File(path + ".config");
        if (proxyConfig.exists()){
            System.out.println("Proxy's config dir yet exists");
        } else {
            if (proxyConfig.mkdir()) {
                System.out.println("Proxy's config dir created");
            } else {
                System.out.println("Failed to create proxy's config dir");
            }
        }
        // try create a file with all files on master
        String proxyMsg = getMasterMsg(inStream);
        try {
            FileOutputStream outputStream = new FileOutputStream(path + ".config/master_files.txt");
            while(!proxyMsg.equals("OK")) {
                outputStream.write((proxyMsg + "\n").getBytes());
                proxyMsg = getMasterMsg(inStream);
            }
            System.out.println("Proxy synchronization ok");
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Sends message to master server.
     * @param msg String message that will be sent.
     * @param outStream Output stream
     */
    private void sendToMaster(String msg, DataOutputStream outStream) {
        try {
            outStream.writeBytes(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Gets responses from master server. It will waits until server sends a response
     *
     * @param inStream Input
     * @return String message from server
     */
    private String getMasterMsg(BufferedReader inStream) {
        String response = "";
        try {
            response = inStream.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
