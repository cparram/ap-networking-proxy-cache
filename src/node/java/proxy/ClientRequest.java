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
public class ClientRequest extends Thread {
    private final String path;
    private final int port;
    private final String ipMaster;
    private final int portMaster;

    public ClientRequest(int port, String path, String ipMaster, int portMaster) {
        this.port = port;
        this.path = path;
        this.ipMaster = ipMaster;
        this.portMaster = portMaster;
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
                        else if (fileExistsOnMaster(fileName)) {
                            if (getFileFromMaster(fileName)){
                                sendFileToClient(fileName, output);
                            } else {
                                removeFileName(fileName);
                                sendToClient("FNE", output);
                            }
                        } else
                            sendToClient("FNE", output);
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
            sendToMaster("GET " + fileName, outStream);
            String masterMsg = getMasterMsg(inStream);

            if (masterMsg.equals("FNE")) { // if response of proxy is: File Not Exists
                System.out.println(fileName + " doesn't exists on master");
                socket.close();
                return false;
            }

            FileOutputStream outputStream = new FileOutputStream(path + fileName);
            while (!masterMsg.equals("EOF")){
                outputStream.write(masterMsg.getBytes());
                masterMsg = getMasterMsg(inStream);
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
     * Sends a file to the client
     * @param fileName
     * @param output
     */
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

    private boolean fileExistsOnMaster(String fileName) {
        try {
            // Note that we read the entire file assuming it will not be too big
            List<String> lines = Files.readAllLines(Paths.get(path + ".config/master_files.txt"), Charset.forName("UTF-8"));
            if (lines.contains(fileName)) {
                System.out.println(fileName + " exists on master server");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(fileName + " doesn't exists on master server");
        return false;
    }

    /**
     * Checks if a file exists or not.
     * @param fileName
     * @return
     */
    private boolean fileExists(String fileName) {
        File file = new File(path + fileName);
        if (file.exists()) {
            System.out.println(fileName + " exists on proxy server");
            return true;
        } else {
            System.out.println(fileName + " doesn't exists on proxy server");
            return false;
        }
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
    private void sendToMaster(String s, DataOutputStream outStream) {
        sendToClient(s, outStream);
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

    private String getMasterMsg(BufferedReader input) {
        return getClientMsg(input);
    }
}
