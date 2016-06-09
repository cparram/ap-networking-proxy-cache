package proxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by cesar on 09-06-16.
 */
public class NodeSync {
    private String path;
    private String ipMaster;
    private Integer portMaster;
    private NodeUtil nodeUtil = new NodeUtil();

    public NodeSync(String path, Integer portMaster, String ipMaster) {
        this.portMaster = portMaster;
        this.ipMaster = ipMaster;
        this.path = path;
    }

    public boolean sync() {
        try {
            Socket socket = new Socket(ipMaster, portMaster);
            // socket bind ip
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            nodeUtil.send("SYNC", outStream);
            nodeUtil.receiveListFile(path + ".config", inStream);
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
