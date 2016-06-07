package proxy;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cesar on 06-06-16.
 */
public class Client {
    @Option(name = "--port", usage = "listening port", required = true)
    private int port;
    @Argument
    private List<String> arguments = new ArrayList<String>();
    private String command;
    private String fileName;

    /**
     * @param args
     */
    public static void main(String[] args) {
        Client client = new Client();

        if (client.config(args)) {
            client.redirectRequestToProxy();
        }
    }

    private void redirectRequestToProxy() {
        try {
            Socket socket = new Socket("localhost", port);
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (command.equals("LIST")){
                sendToProxyServer(command , outStream);
                receiveListFile(inStream);
            } else {
                sendToProxyServer(command + " " + fileName , outStream);
                if (command.equals("GET")){
                    receiveFileFromProxy(inStream);
                } else if (command.equals("PUT")) {
                    sendFileToProxy(fileName, outStream, inStream);
                    System.out.println(getProxyServerMsg(inStream));
                } else if (command.equals("DELETE")){
                    System.out.println(getProxyServerMsg(inStream));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendFileToProxy(String fileName, DataOutputStream outStream, BufferedReader inStream) {
        FileInputStream clientFile;
        String proxyMsg = getProxyServerMsg(inStream);
        if (proxyMsg.equals("FYE")) { // if response of proxy is: File Yet Exists
            System.out.println(proxyMsg);
            return;
        } else if (proxyMsg.equals("FNE")) { // only put a file if not exists
            try {
                clientFile = new FileInputStream(fileName);
                byte[] buffer = new byte[1024];
                while (clientFile.read(buffer) != -1) {
                    sendToProxyServer(new String(buffer), outStream);
                }
                sendToProxyServer("EOF", outStream);
                clientFile.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets file from proxy server
     * @param inStream
     */
    private void receiveFileFromProxy(BufferedReader inStream) {
        String proxyMsg = getProxyServerMsg(inStream);
        FileOutputStream outputStream;
        if (proxyMsg.equals("FNE")) { // if response of proxy is: File Not Exists
            System.out.println("File not exists on server");
            return;
        }
        try {
            outputStream = new FileOutputStream(fileName);
            while (!proxyMsg.equals("EOF")){
                outputStream.write(proxyMsg.getBytes());
                proxyMsg = getProxyServerMsg(inStream);
            }
            System.out.println("File received");
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the list of available files on proxy server
     * @param inStream
     */
    private void receiveListFile(BufferedReader inStream) {
        String proxyMsg = getProxyServerMsg(inStream);
        while(!proxyMsg.equals("OK")) {
            System.out.println(proxyMsg);
            proxyMsg = getProxyServerMsg(inStream);
        }
    }

    /**
     * Sends message to proxy server.
     * @param msg String message that will be sent.
     * @param outStream Output stream
     */
    private void sendToProxyServer(String msg, DataOutputStream outStream) {
        try {
            outStream.writeBytes(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Gets responses from proxy server. It will waits until server sends a response
     *
     * @param inStream Input
     * @return String message from server
     */
    private String getProxyServerMsg(BufferedReader inStream) {
        String response = "";
        try {
            response = inStream.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private boolean config(String[] args) {
        boolean configured = false;
        CmdLineParser parser = new CmdLineParser(this);
        try {
            // parse the arguments.
            parser.parseArgument(args);

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
        }

        if (arguments.size() <= 2) {
            command = arguments.get(0);
            if (command.equals("LIST")){
                configured = arguments.size() == 1;
            } else {
                if (arguments.size() == 2) {
                    fileName = arguments.get(1);
                    configured = true;
                }
            }
        }
        return configured;
    }
}
