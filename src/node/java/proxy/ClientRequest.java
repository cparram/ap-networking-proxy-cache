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
                String clientMsg = receive(input);
                System.out.println("Message from client: '" + clientMsg +"'");
                if (clientMsg.equals("LIST")) {
                    sendListFilesToClient(output);
                } else {
                    String action = clientMsg.split(" ")[0];
                    String fileName = clientMsg.split(" ")[1];
                    if (action.equals("GET")) {
                        if (fileExists(fileName))
                            sendFile(fileName, output);
                        else if (fileExistsOnConfig(fileName)) {
                            if (getFileFromMaster(fileName)){
                                sendFile(fileName, output);
                            } else {
                                removeFileName(fileName);
                                send("FNE", output);
                            }
                        } else
                            send("FNE", output);
                    } else if (action.equals("PUT")) {
                        if (fileExists(fileName)) {
                            send("FYE", output);
                        } else if (fileExistsOnConfig(fileName)) {
                            // anomal case
                            send("FYE", output);
                        } else {
                            send("FNE", output);
                            if (receiveFile(fileName, input)) {
                                if (sendFileToMaster(fileName)) {
                                    send("OK", output);
                                } else {
                                    send("FAIL", output);
                                }
                            } else {
                                send("FAIL", output);
                            }

                        }
                    }
                    // // ToDo: implement other responses
                    // else if (action.equals("DELETE")) {
                    //    if (fileExists(fileName))
                    //        removeMasterFile(fileName);
                    // }
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean sendFileToMaster(String fileName) {
        try {
            Socket socket = new Socket(ipMaster, portMaster);
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            send("PUT " + fileName, outStream);
            String masterMsg = receive(inStream);

            if (masterMsg.equals("FYE")) { // if response of master is: File Not Exists
                System.out.println(fileName + " already exists on master");
                socket.close();
                return false;
            }
            sendFile(fileName, outStream);
            masterMsg = receive(inStream);
            socket.close();

            if (masterMsg.equals("OK")) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    private boolean receiveFile(String fileName, BufferedReader input) {
        try {
            String clientMsg = receive(input);
            FileOutputStream outputStream = new FileOutputStream(path + fileName);
            while (!clientMsg.equals("EOF")){
                outputStream.write(clientMsg.getBytes());
                clientMsg = receive(input);
            }
            System.out.println(fileName + " received from client");
            outputStream.close();
            return true;
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
            send("GET " + fileName, outStream);
            String masterMsg = receive(inStream);

            if (masterMsg.equals("FNE")) { // if response of proxy is: File Not Exists
                System.out.println(fileName + " doesn't exists on master");
                socket.close();
                return false;
            }

            FileOutputStream outputStream = new FileOutputStream(path + fileName);
            while (!masterMsg.equals("EOF")){
                outputStream.write(masterMsg.getBytes());
                masterMsg = receive(inStream);
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

    /**
     * Sends to client a list of files
     * @param output
     */
    private void sendListFilesToClient(DataOutputStream output) {
        File file = new File(path + ".config/master_files.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                send(line, output);
            }
            send("OK", output);
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
    private void send(String msg, DataOutputStream output) {
        try {
            output.writeBytes(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets client message. It will wait until client sends a message
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

}
