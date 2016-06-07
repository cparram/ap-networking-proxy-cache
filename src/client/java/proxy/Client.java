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
                send(command , outStream);
                receiveListFile(inStream);
            } else {
                if (command.equals("GET")){
                    send(command + " " + fileName , outStream);
                    receive(inStream);
                } else if (command.equals("PUT")) {
                    if (existsFile(fileName)) {
                        send(command + " " + fileName , outStream);
                        sendFile(fileName, outStream, inStream);
                    } else {
                        System.out.println(fileName + " doesn't exists");
                    }
                } else if (command.equals("DELETE")) {
                    send(command + " " + fileName, outStream);
                    String proxyMsg = getProxyServerMsg(inStream);
                    if (proxyMsg.equals("FNE")) {
                        System.out.println(fileName + " doesn't exist on server");
                    } else if (proxyMsg.equals("OK")) {
                        System.out.println(fileName + " deleted");
                    }
                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean existsFile(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    private void sendFile(String fileName, DataOutputStream outStream, BufferedReader inStream) {
        FileInputStream clientFile;
        String proxyMsg = getProxyServerMsg(inStream);
        if (proxyMsg.equals("FYE")) { // if response of proxy is: File Yet Exists
            System.out.println(fileName + " already exists on server");
            return;
        } else if (proxyMsg.equals("FNE")) { // only put a file if not exists
            try {
                clientFile = new FileInputStream(fileName);
                byte[] buffer = new byte[1024];
                while (clientFile.read(buffer) != -1) {
                    send(new String(buffer), outStream);
                }
                send("EOF", outStream);
                clientFile.close();
                proxyMsg = getProxyServerMsg(inStream);
                if (proxyMsg.equals("OK")) {
                    System.out.println(fileName + " correctly put on server");
                } else {
                    System.out.println(fileName + " doesn't put on server");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets file from proxy server
     * @param inStream
     */
    private void receive(BufferedReader inStream) {
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
    private void send(String msg, DataOutputStream outStream) {
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
