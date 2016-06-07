package proxy;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Created by cesar on 06-06-16.
 */
public class Node {

    @Option(name = "--port-proxy-client", usage = "listening proxy's port to client", required = true)
    private int portProxyClient;
    @Option(name = "--port-proxy-master", usage = "listening proxy's port to master", required = true)
    private int portProxyMaster;
    @Option(name = "--port-master", usage = "listening master's port to proxys", required = true)
    private int portMaster;
    @Option(name = "--ip-master", usage = "Master's ip", required = true)
    private String ipMaster;
    @Option(name = "--path", usage = "Path to files", required = true)
    private String path;

    public static void main(String[] args) {
        Node node = new Node();

        if (node.config(args)) {
            node.listenClientRequests();
            node.sinchronizeAndListenMaster();
        }
    }

    /**
     * Brings the list of files that the master have
     */
    private void sinchronizeAndListenMaster() {
        (new MasterRequest(portMaster, ipMaster, path, portProxyMaster)).start();
    }

    /**
     * Creates a thread that listen requests from client.
     */
    private void listenClientRequests() {
        (new ClientRequest(portProxyClient, path, ipMaster, portMaster)).start();
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
