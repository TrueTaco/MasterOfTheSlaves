import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.ArrayList;

public class MasterSlave implements Runnable {

    private long pid;
    private long tid;

    private static final int maxIncomingClients = 100;
    private String type;
    private int slavePort;
    private int masterPort;
    private int masterConnectionPort;
    private ArrayList<Node> NodeList = new ArrayList<>();

    public ObjectOutputStream slaveMasterObjectOutputStream;

    public MasterSlave(String type, int masterPort) {
        this.type = type;
        if (type.equals("Master")) {
            this.masterPort = masterPort;
        } else {
            throw new java.lang.Error("This constructor is used for Master");
        }
    }

    public MasterSlave(String type, int slavePort, int masterConnectionPort) {
        this.type = type;
        if (type.equals("Slave")) {
            this.slavePort = slavePort;
            this.masterConnectionPort = masterConnectionPort;
        } else {
            throw new java.lang.Error("This constructor is used for Slave");
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        if (type.equals("Slave")) {
            try {
                boolean discovered = false;
                // Client Socket
                ServerSocket clientServerSocket = new ServerSocket(slavePort, 100);

                ConnectionThread runnableConnectionThread = new ConnectionThread(clientServerSocket, this);
                Thread newConnectionThread = new Thread(runnableConnectionThread);
                newConnectionThread.start();

                // Slave Socket
                Socket slaveServerSocket = initialiseClient(masterConnectionPort, "localhost");

                OutputStream slaveMasterOutputStream = slaveServerSocket.getOutputStream();
                ObjectOutputStream slaveMasterObjectOutputStream = new ObjectOutputStream(slaveMasterOutputStream);
                this.slaveMasterObjectOutputStream = slaveMasterObjectOutputStream;

                InputStream slaveMasterInputStream = slaveServerSocket.getInputStream();
                ObjectInputStream slaveMasterObjectInputStream = new ObjectInputStream(slaveMasterInputStream);

                System.out.println("Slave " + pid + "-" + tid + " is ready");

                while (true) {
                    // DISCORVERY
                    if (!discovered) {
                        Message message = read(slaveMasterObjectInputStream);
                        if (message.getType().equals("DISCOVERY")) {
                            replyDiscovery(slaveMasterObjectOutputStream);
                        }
                        discovered = true;
                    }
                    System.out.println("Slave " + pid + "-" + tid + ": Calling function in ConnectionThread for forwarding");
                    Message message = read(slaveMasterObjectInputStream);
                    runnableConnectionThread.forward(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (type.equals("Master")) {
            try {
                ServerSocket masterServerSocket = new ServerSocket(masterPort, 100);
                System.out.println("Master " + pid + "-" + tid + " is ready");

                // Connects to new slaves
                while (true) {
                    Socket newSlaveConnection = masterServerSocket.accept();

                    SlaveHandler newSlaveHandler = new SlaveHandler(newSlaveConnection);
                    Thread newThread = new Thread(newSlaveHandler);
                    newThread.start();

                    // Adds node to list
                    System.out.println(newSlaveConnection.getLocalPort());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Socket initialiseServer(String dns, int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(
                port,
                maxIncomingClients,
                InetAddress.getByName(dns));
        Socket clientCommSocket = serverSocket.accept();
        return clientCommSocket;
    }

    public Socket initialiseClient(int port, String dns) throws IOException {
        Socket cs = new Socket(dns, port);

        return cs;
    }

    public Message read(ObjectInputStream ois) {
        Message ret = null;
        try {
            ret = (Message) ois.readObject();
        } catch (Exception e) {
            System.err.println(e.toString());

        }
        return ret;
    }

    public Message sendMessage(String type, String payload) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        return message;
    }

    public void replyDiscovery(ObjectOutputStream objectOutputStream) throws IOException {
        System.out.println("Slave " + pid + "-" + tid + ": Discovery request received");
        Node node = new Node((pid + "-" + tid), slavePort, type);
        Message nodeMessage = new Message();
        nodeMessage.setType("DISCOVERY-RESPONSE");
        nodeMessage.setPayload(node);
        objectOutputStream.writeObject(nodeMessage);
        System.out.println("Slave " + pid + "-" + tid + ": Discovery response send");
    }

    public void forward(Message message) throws IOException {
        // FORWARDING TO MASTER
        this.slaveMasterObjectOutputStream.writeObject(message);
        System.out.println("Slave " + pid + "-" + tid + ": Forwarding message to Master");
    }
}


