package proxy;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Created by cesar on 07-06-16.
 */
public class Master {
    @Option(name = "--port-proxy-master", usage = "listening proxy's port to master", required = true)
    private int portProxyMaster;
    @Option(name = "--port-master", usage = "listening master's port to proxys", required = true)
    private int portMaster;
    @Option(name = "--path", usage = "Path to files", required = true)
    private String path;

    public static void main(String[] args) {
        Master master = new Master();

        if (master.config(args)) {
            master.listenProxyRequests();
        }
    }

    private void listenProxyRequests() {
        try {
            ServerSocket serverSocket = new ServerSocket(portMaster);

            while(true) {
                Socket socket = serverSocket.accept();
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String ip = socket.getRemoteSocketAddress().toString().replace("/", "").split(":")[0];
                saveProxyIp(ip);

                String proxyMsg = receive(input);
                System.out.println("Message received from a proxy: '" + proxyMsg + "'");
                if (proxyMsg.equals("SYNC")) {
                    sendListFiles(output);
                } else {
                    String action = proxyMsg.split(" ")[0];
                    String fileName = proxyMsg.split(" ")[1];
                    if (action.equals("GET")) {
                        if (fileExists(fileName))
                            sendFile(fileName, output);
                        else {
                            send("FNE", output);
                        }
                    } else if (action.equals("PUT")) {
                        if(fileExists(fileName)) {
                            send("FYE", output);
                        } else {
                            send("FNE", output);
                            receiveFile(fileName, input);
                            if (updateProxysFiles()){
                                send("OK", output);
                            } else {
                                send("FAIL", output);
                            }
                        }
                    } else if (action.equals("DELETE")) {
                        if(fileExists(fileName)) {
                            if(deleteFile(fileName)){
                                if (cascadeDeletion(fileName) && updateProxysFiles()){
                                    send("OK", output);
                                } else {
                                    send("FAIL", output);
                                }
                            } else {
                                send("FAIL", output);
                            }
                        } else {
                            send("FNE", output);
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
            for (String ip : proxys) {
                Socket socket = new Socket(ip, portProxyMaster);
                DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                send("DELETE " + fileName, outStream);
                String proxyMsg = receive(inStream);
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

    private boolean deleteFile(String fileName) {
        File file = new File(path + fileName);
        return file.delete();
    }

    private boolean updateProxysFiles() {
        String ipsFilePath = path + ".config/ips.txt";
        try {
            // Note that we read the entire file assuming it will not be too big
            List<String> proxys = Files.readAllLines(Paths.get(ipsFilePath), Charset.forName("UTF-8"));
            for (String ip : proxys) {
                Socket socket = new Socket(ip, portProxyMaster);
                DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                send("SYNC", outStream);
                sendListFiles(outStream);
                String proxyMsg = receive(inStream);
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

    private void receiveFile(String fileName, BufferedReader input) {
        try {
            String proxyMsg = receive(input);
            FileOutputStream outputStream = new FileOutputStream(path + fileName);
            while (!proxyMsg.equals("EOF")){
                outputStream.write(proxyMsg.getBytes());
                proxyMsg = receive(input);
            }
            System.out.println(fileName + " received from proxy");
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String fileName, DataOutputStream output) {
        FileInputStream serverFile;
        try {
            serverFile = new FileInputStream(path + fileName);
            byte[] buffer = new byte[1024];
            while (serverFile.read(buffer) != -1) {
                send(new String(buffer), output);
            }
            send("EOF", output);
            serverFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean fileExists(String fileName) {
        File file = new File(path + fileName);
        return file.exists();
    }

    private void sendListFiles(DataOutputStream output) {
        File folder = new File(path);
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile())
                send(fileEntry.getName(), output);
        }
        send("OK", output);
    }

    /**
     * Save the proxy ip on a config file
     * @param ipProxy
     */
    private void saveProxyIp(String ipProxy) {
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

    /**
     * Sends messages to proxy.
     *
     * @param msg    Message that will be sent
     * @param output Data output stream
     */
    private void send(String msg, DataOutputStream output) {
        try {
            output.writeBytes(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets proxy message. It will wait until client sends a message
     *
     * @param input Reader
     * @return String message from client
     */
    private String receive(BufferedReader input) {
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
