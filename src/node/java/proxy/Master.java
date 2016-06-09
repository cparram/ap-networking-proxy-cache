package proxy;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Created by cesar on 08-06-16.
 */
public class Master extends Thread {
    private final static int PORT=8181;
    private final int port;
    private String path;
    private NodeUtil nodeUtil = new NodeUtil();

    public Master(String path, int port) {
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
                String ip = socket.getRemoteSocketAddress().toString().replace("/", "").split(":")[0];
                saveProxyIp(ip);

                String proxyMsg = nodeUtil.receive(input);
                System.out.println("Message received from a proxy: '" + proxyMsg + "'");
                if (proxyMsg.equals("SYNC")) {
                    sendListFiles(output);
                } else {
                    String action = proxyMsg.split(" ")[0];
                    String fileName = proxyMsg.split(" ")[1];
                    if (action.equals("GET")) {
                        if (nodeUtil.fileExists(path + fileName))
                            nodeUtil.sendFile(path + fileName, output);
                        else {
                            nodeUtil.send("FNE", output);
                        }
                    } else if (action.equals("PUT")) {
                        if(nodeUtil.fileExists(path + fileName)) {
                            nodeUtil.send("FYE", output);
                        } else {
                            nodeUtil.send("FNE", output);
                            nodeUtil.receiveFile(path + fileName, input);
                            if (updateProxysFiles()){
                                nodeUtil. send("OK", output);
                            } else {
                                nodeUtil.send("FAIL", output);
                            }
                        }
                    } else if (action.equals("DELETE")) {
                        if(nodeUtil.fileExists(path + fileName)) {
                            if(nodeUtil.deleteFile(path + fileName)){
                                if (cascadeDeletion(fileName) && updateProxysFiles()){
                                    nodeUtil.send("OK", output);
                                } else {
                                    nodeUtil.send("FAIL", output);
                                }
                            } else {
                                nodeUtil.send("FAIL", output);
                            }
                        } else {
                            nodeUtil.send("FNE", output);
                        }
                    }

                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean cascadeDeletion(String fileName) {
        String ipsFilePath = path + ".config/ips.txt";
        try {
            // Note that we read the entire file assuming it will not be too big
            List<String> proxys = Files.readAllLines(Paths.get(ipsFilePath), Charset.forName("UTF-8"));
            for (String _ip : proxys) {
                Socket socket = new Socket(_ip, PORT);
                DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                nodeUtil.send("DELETE " + fileName, outStream);
                String proxyMsg = nodeUtil.receive(inStream);
                socket.close();
                if (proxyMsg.equals("FAIL")){
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean updateProxysFiles() {
        String ipsFilePath = path + ".config/ips.txt";
        try {
            // Note that we read the entire file assuming it will not be too big
            List<String> proxys = Files.readAllLines(Paths.get(ipsFilePath), Charset.forName("UTF-8"));
            for (String _ip : proxys) {
                Socket socket = new Socket(_ip, PORT);
                DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                nodeUtil.send("SYNC", outStream);
                sendListFiles(outStream);
                String proxyMsg = nodeUtil.receive(inStream);
                socket.close();
                if (!proxyMsg.equals("OK")){
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void sendListFiles(DataOutputStream output) {
        File folder = new File(path);
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile())
                nodeUtil.send(fileEntry.getName(), output);
        }
        nodeUtil.send("OK", output);
    }

    /**
     * Save the proxy ip on a config file
     * @param ipProxy
     */
    private void saveProxyIp(String ipProxy) {
        System.out.println("Ip that master will save: " + ipProxy);
        // Creation of config dir
        File masterConfig = new File(path + ".config");
        if (masterConfig.exists()){
            System.out.println("Master's config dir yet exists");
        } else {
            if (masterConfig.mkdir()) {
                System.out.println("Master's config dir created");
            } else {
                System.out.println("Failed to create Master's config dir");
            }
        }


        String ipsFilePath = path + ".config/ips.txt";
        // It ensures the existence of the file ips
        File ipsFile = new File(ipsFilePath);
        if (!ipsFile.exists()){
            try {
                ipsFile.createNewFile();
                System.out.println("ips file created");
            } catch (IOException e) {
                System.out.println("Failed to create ips file");
                e.printStackTrace();
            }
        }
        try {
            // Note that we read the entire file assuming it will not be too big
            List<String> lines = Files.readAllLines(Paths.get(ipsFilePath), Charset.forName("UTF-8"));
            if (!lines.contains(ipProxy)) {
                Files.write(Paths.get(ipsFilePath), (ipProxy + "\n").getBytes(), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
