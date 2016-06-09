package proxy;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by cesar on 06-06-16.
 */
public class Node {

    private Integer portMaster;
    private String ipMaster;
    public Boolean isMaster;

    @Option(name = "--path", usage = "Path to files", required = true)
    private String path;
    @Option(name = "--port", usage = "Listen port", required = true)
    private int port;

    public static void main(String[] args) {
        Node node = new Node();

        if (node.config(args)) {
            if (node.isMaster) {
                System.out.println("Node is master");
                node.listenNodes();
            } else {
                System.out.println("Node is proxy");
                if (node.sync()) {
                    node.listenClient();
                    node.listenMaster();
                } else {
                    System.out.println("Proxy can't sync with master");
                }
            }
        }
    }

    private boolean sync() {
        NodeSync nodeSync = new NodeSync(path, portMaster, ipMaster);
        return nodeSync.sync();
    }

    private void listenNodes() {
        Master master = new Master(path, port);
        master.start();
    }

    /**
     * Brings the list of files that the master have
     */
    private void listenMaster() {
        ProxyMaster proxyMaster = new ProxyMaster(path);
        proxyMaster.start();
    }

    /**
     * Creates a thread that listen requests from client.
     */
    private void listenClient() {
        ProxyClient proxyClient = new ProxyClient(path, port, portMaster, ipMaster);
        proxyClient.start();
    }


    private boolean config(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            // parse the arguments.
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            return false;
        }
        path = path.endsWith("/") ? path : path + "/";
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("topology.properties"));
            System.out.println("master port: " + props.get("master.port"));
            portMaster = Integer.valueOf(props.get("master.port").toString());
            ipMaster = (String) props.get("master.ip");
            if (portMaster == null || ipMaster == null)
                return false;
            isMaster = (port == portMaster);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
