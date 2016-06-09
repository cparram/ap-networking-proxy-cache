package proxy;

import java.io.*;

/**
 * Created by cesar on 09-06-16.
 */
public class NodeUtil {
    /**
     * Removes a file
     * @param fileName
     * @return True if can remove file, otherwise False.
     */
    public boolean deleteFile(String fileName) {
        File file = new File(fileName);
        return file.delete();
    }

    /**
     * Checks if a file exists or not.
     * @param fileName
     * @return
     */
    public boolean fileExists(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            System.out.println(fileName + " exists");
            return true;
        } else {
            System.out.println(fileName + " doesn't exists");
            return false;
        }
    }

    /**
     * Sends a file
     * @param fileName
     * @param output
     */
    public boolean sendFile(String fileName, DataOutputStream output) {
        FileInputStream serverFile;
        try {
            serverFile = new FileInputStream(fileName);
            byte[] buffer = new byte[1024];
            while (serverFile.read(buffer) != -1) {
                send(new String(buffer), output);
            }
            send("EOF", output);
            serverFile.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets a list of files. It also checks if config dir exist.
     * @param configDir
     * @param inStream
     */
    public void receiveListFile(String configDir, BufferedReader inStream) {
        // Creation of config dir
        File proxyConfig = new File(configDir);
        if (proxyConfig.exists()){
            System.out.println(" Proxy's config dir yet exists");
        } else {
            if (proxyConfig.mkdir()) {
                System.out.println("Proxy's config dir created");
            } else {
                System.out.println("Failed to create proxy's config dir");
            }
        }
        // try create a file with all files on master
        String proxyMsg = receive(inStream);
        try {
            FileOutputStream outputStream = new FileOutputStream(configDir + "/master_files.txt");
            while(!proxyMsg.equals("OK")) {
                outputStream.write((proxyMsg + "\n").getBytes());
                proxyMsg = receive(inStream);
            }
            System.out.println("Proxy's synchronization ok");
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Gets a file
     * @param fileName Where the file is stored
     * @param input
     * @return True if the file is received, otherwise False
     */
    public boolean receiveFile(String fileName, BufferedReader input) {
        try {
            String clientMsg = receive(input);
            FileOutputStream outputStream = new FileOutputStream(fileName);
            while (!clientMsg.equals("EOF")){
                outputStream.write(clientMsg.getBytes());
                clientMsg = receive(input);
            }
            System.out.println(fileName + " received");
            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sends messages to client.
     *
     * @param msg    Message that will be sent
     * @param output Data output stream
     */
    public void send(String msg, DataOutputStream output) {
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
    public String receive(BufferedReader input) {
        String msg = "";
        try {
            msg = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }
}
