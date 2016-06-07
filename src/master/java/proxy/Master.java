package proxy;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
                saveProxyIp(socket.getRemoteSocketAddress().toString());

                String proxyMsg = getProxyMsg(input);
                if (proxyMsg.equals("SINCRONIZE")) {
                    sendListFilesToProxy(output);
                } else {
                    // ToDo: implement other responses
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void sendListFilesToProxy(DataOutputStream output) {
        File folder = new File(path);
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile())
                sendToProxy(fileEntry.getName(), output);
        }
        sendToProxy("OK", output);
    }

    /**
     * Save the proxy ip on a config file
     * @param ipProxyWithPort
     */
    private void saveProxyIp(String ipProxyWithPort) {
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
        String ipProxy = ipProxyWithPort.split(":")[0];
        try {
            String content = new String(Files.readAllBytes(Paths.get(ipsFilePath)));
            boolean included = false;
            for (String ip: content.split("/") ) {
                if (ip.equals(ipProxy.replace("/", "")))
                    included = true;
            }
            if (!included) {
                Files.write(Paths.get(ipsFilePath), ipProxy.getBytes(), StandardOpenOption.APPEND);
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
    private void sendToProxy(String msg, DataOutputStream output) {
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
    private String getProxyMsg(BufferedReader input) {
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
