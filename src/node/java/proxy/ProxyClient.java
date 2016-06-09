package proxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Created by cesar on 07-06-16.
 */
public class ProxyClient extends Thread {

    private int port;
    private String path;
    private String ipMaster;
    private int portMaster;
    private NodeUtil nodeUtil = new NodeUtil();

    public ProxyClient(String path, int port, Integer portMaster, String ipMaster) {
        this.path = path;
        this.port = port;
        this.portMaster = portMaster;
        this.ipMaster = ipMaster;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while(true) {
                Socket socket = serverSocket.accept();
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String clientMsg = nodeUtil.receive(input);
                System.out.println("Message from client: '" + clientMsg +"'");
                if (clientMsg.equals("LIST")) {
                    sendListFilesToClient(output);
                } else {
                    String action = clientMsg.split(" ")[0];
                    String fileName = clientMsg.split(" ")[1];
                    if (action.equals("GET")) {
                        if (nodeUtil.fileExists(path + fileName))
                            nodeUtil.sendFile(path + fileName, output);
                        else if (fileExistsOnConfig(fileName)) {
                            if (getFileFromMaster(fileName)){
                                nodeUtil.sendFile(path + fileName, output);
                            } else {
                                removeFileName(fileName);
                                nodeUtil.send("FNE", output);
                            }
                        } else
                            nodeUtil.send("FNE", output);
                    } else if (action.equals("PUT")) {
                        if (nodeUtil.fileExists(path + fileName)) {
                            nodeUtil.send("FYE", output);
                        } else if (fileExistsOnConfig(fileName)) {
                            // anomal case
                            nodeUtil.send("FYE", output);
                        } else {
                            nodeUtil.send("FNE", output);
                            if (nodeUtil.receiveFile(path + fileName, input)) {
                                if (sendFileToMaster(fileName)) {
                                    nodeUtil.send("OK", output);
                                } else {
                                    nodeUtil.send("FAIL", output);
                                }
                            } else {
                                nodeUtil.send("FAIL", output);
                            }

                        }
                    } else if (action.equals("DELETE")) {
                        if (fileExistsOnConfig(fileName)) {
                            if (removeMasterFile(fileName)) {
                                nodeUtil.send("OK", output);
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

    private boolean removeMasterFile(String fileName) {
        try {
            Socket socket = new Socket(ipMaster, portMaster);
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            nodeUtil.send("DELETE " + fileName, outStream);
            String masterMsg = nodeUtil.receive(inStream);
            socket.close();

            if (masterMsg.equals("FNE")) { // if response of master is: File Not Exists
                System.out.println(fileName + " not exists on master");
                return false;
            } else if (masterMsg.equals("OK"))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean sendFileToMaster(String fileName) {
        try {
            Socket socket = new Socket(ipMaster, portMaster);
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            nodeUtil.send("PUT " + fileName, outStream);
            String masterMsg = nodeUtil.receive(inStream);

            if (masterMsg.equals("FYE")) { // if response of master is: File Not Exists
                System.out.println(fileName + " already exists on master");
                socket.close();
                return false;
            }
            nodeUtil.sendFile(path + fileName, outStream);
            masterMsg = nodeUtil.receive(inStream);
            socket.close();

            if (masterMsg.equals("OK")) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Removes file name of config file
     * @param fileName
     */
    private void removeFileName(String fileName) {
        Path masterFile = Paths.get(path + ".config/master_files.txt");
        try {
            // Note that
            // we read the entire file assuming it will not be too big
            List<String> lines = Files.readAllLines(masterFile, Charset.forName("UTF-8"));
            lines.remove(fileName);

            // delete file and create new one
            masterFile.toFile().delete();
            masterFile.toFile().createNewFile();
            for (String line : lines) {
                Files.write(masterFile, (line + "\n").getBytes(), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new socket to request a file to Master
     * @param fileName
     * @return
     */
    private boolean getFileFromMaster(String fileName) {
        try {
            Socket socket = new Socket(ipMaster, portMaster);
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            nodeUtil.send("GET " + fileName, outStream);
            String masterMsg = nodeUtil.receive(inStream);

            if (masterMsg.equals("FNE")) { // if response of proxy is: File Not Exists
                System.out.println(fileName + " doesn't exists on master");
                socket.close();
                return false;
            }

            FileOutputStream outputStream = new FileOutputStream(path + fileName);
            while (!masterMsg.equals("EOF")){
                outputStream.write(masterMsg.getBytes());
                masterMsg = nodeUtil.receive(inStream);
            }
            System.out.println(fileName + " received from master");
            outputStream.close();
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
                nodeUtil.send(line, output);
            }
            nodeUtil.send("OK", output);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean fileExistsOnConfig(String fileName) {
        try {
            // Note that we read the entire file assuming it will not be too big
            List<String> lines = Files.readAllLines(Paths.get(path + ".config/master_files.txt"), Charset.forName("UTF-8"));
            if (lines.contains(fileName)) {
                System.out.println(fileName + " exists on configuration file");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(fileName + " doesn't exists on configuration file");
        return false;
    }

}
