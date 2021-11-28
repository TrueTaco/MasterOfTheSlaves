import resources.RSAHelper;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;

public class MasterSlave implements Runnable {

    private long pid;
    private long tid;

    private static final int maxIncomingClients = 100;
    private String type;
    private int slavePort;
    private int masterPort;
    private int masterConnectionPort;
    private ArrayList<Node> NodeList = new ArrayList<>();
    public HashMap<SlaveHandler, Thread> threads = new HashMap<>();
    public long firstPointInTime;

    public ArrayList<SlaveHandler> slaveHandlerList = new ArrayList<>();

    public ObjectOutputStream slaveMasterObjectOutputStream;

    public String publicKey;
    public String chiffre;
    public String amountOfPrimes;

    private Thread WorkingThread;
    private boolean closeSlave = false;


    // Constructor for master
    public MasterSlave(String type, int masterPort) {
        this.type = type;
        if (type.equals("Master")) {
            this.masterPort = masterPort;
        } else {
            throw new java.lang.Error("This constructor is used for Master");
        }
    }

    // Constructor for slave
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
                // Creates Client Socket ands start connection thread
                ServerSocket clientServerSocket = new ServerSocket(slavePort, 100);

                ConnectionThread runnableConnectionThread = new ConnectionThread(clientServerSocket, this);
                Thread newConnectionThread = new Thread(runnableConnectionThread);
                newConnectionThread.start();

                // Creates Slave Socket
                Socket slaveServerSocket = initialiseClient(masterConnectionPort, "localhost");

                OutputStream slaveMasterOutputStream = slaveServerSocket.getOutputStream();
                ObjectOutputStream slaveMasterObjectOutputStream = new ObjectOutputStream(slaveMasterOutputStream);
                this.slaveMasterObjectOutputStream = slaveMasterObjectOutputStream;

                InputStream slaveMasterInputStream = slaveServerSocket.getInputStream();
                ObjectInputStream slaveMasterObjectInputStream = new ObjectInputStream(slaveMasterInputStream);

                System.out.println("Slave " + pid + "-" + tid + " is ready");

                while (true) {

                    // Check if the node is already discovery
                    if (!discovered) {
                        Message message = read(slaveMasterObjectInputStream);
                        if (message.getType().equals("DISCOVERY")) {
                            replyDiscovery(slaveMasterObjectOutputStream);
                        }
                        discovered = true;
                    }
                    Message message = read(slaveMasterObjectInputStream);

                    // If message type equals New List, safe the received list
                    if (message.getType().equals("NEW LIST")) {
                        this.NodeList = (ArrayList<Node>) message.getPayload();
                        String list = "";
                        for (Node element : ((ArrayList<Node>) message.getPayload())) {
                            list += element.getID() + ", ";
                        }
                        System.out.println("\nSlave " + pid + "-" + tid + ": " + "New nodelist received: " + list);

                    // If message type equals heartbeat, create and send message as response
                    } else if (message.getType().equals("HEARTBEAT")) {
                        Message newMessage = new Message();
                        newMessage.setType("HEARTBEAT-RESPONSE");
                        slaveMasterObjectOutputStream.writeObject(newMessage);

                    // If message type equals RSA information, parse the information and start working slave with given parameters
                    } else if (message.getType().equals("RSA-INFORMATION")) {
                        System.out.println("Slave " + pid + "-" + tid + " received: RSA information");
                        ArrayList<String> rsaInformation = (ArrayList<String>) message.getPayload();

                        String slavePublicKey = rsaInformation.get(0);
                        int startIndex = Integer.parseInt(rsaInformation.get(1));
                        int endIndex = Integer.parseInt(rsaInformation.get(2));
                        rsaInformation.remove(2);
                        rsaInformation.remove(1);
                        rsaInformation.remove(0);
                        ArrayList<String> slavePrimes = rsaInformation;

                        WorkingThread runnableWorkingThread = new WorkingThread(slavePublicKey, startIndex, endIndex, slavePrimes, this);
                        Thread newWorkingThread = new Thread(runnableWorkingThread);
                        WorkingThread = newWorkingThread;
                        newWorkingThread.start();

                    // If no matching message type is found forward message to Client
                    } else {
                        System.out.println("Slave " + pid + "-" + tid + ": Calling function in ConnectionThread for forwarding");
                        runnableConnectionThread.forward(message);
                    }
                    if (closeSlave) {
                        WorkingThread.join();
                        System.out.println("Slave " + pid + "-" + tid + ": killed my WorkingThread");
                        closeSlave = false;
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else if (type.equals("Master")) {
            try {
                ServerSocket masterServerSocket = new ServerSocket(masterPort, 100);
                System.out.println("Master " + pid + "-" + tid + " is ready");


                Timer timer = new Timer();
                timer.schedule(new ConnectionChecker(this), 5000, 5000);

                // Connects to new slaves
                while (true) {
                    Socket newSlaveConnection = masterServerSocket.accept();

                    SlaveHandler newSlaveHandler = new SlaveHandler(newSlaveConnection, this);
                    Thread newThread = new Thread(newSlaveHandler);
                    threads.put(newSlaveHandler, newThread);
                    this.slaveHandlerList.add(newSlaveHandler);

                    newThread.start();
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

    // Reads given objectInputStream and returns the read message
    public Message read(ObjectInputStream ois) {
        Message ret = null;
        try {
            ret = (Message) ois.readObject();
        } catch (Exception e) {
            System.err.println(e.toString());

        }
        return ret;
    }

    // Returns message with text message as payload depending on the input type and payload
    public Message sendMessage(String type, String payload) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        return message;
    }

    // Replies discovery response by sending a node with information about itseld
    public void replyDiscovery(ObjectOutputStream objectOutputStream) throws IOException {
        System.out.println("Slave " + pid + "-" + tid + ": Discovery request received");
        Node node = new Node((pid + "-" + tid), slavePort, type);
        Message nodeMessage = new Message();
        nodeMessage.setType("DISCOVERY-RESPONSE");
        nodeMessage.setPayload(node);
        objectOutputStream.writeObject(nodeMessage);
        System.out.println("Slave " + pid + "-" + tid + ": Discovery response send");
    }

    // Sends message to be forwarded to master
    public void forward(Message message) throws IOException {
        // FORWARDING TO MASTER
        this.slaveMasterObjectOutputStream.writeObject(message);
        System.out.println("Slave " + pid + "-" + tid + ": Forwarding message to Master");
    }

    // Adds node to own NodeList and distributes it
    public void addNode(Node node) throws IOException {
        this.NodeList.add(node);
        for (SlaveHandler slaveHandler : slaveHandlerList) {
            slaveHandler.sendNewList(NodeList);
        }
    }

    // Processes needed ranges for primes and instructs the slavehandler to send information to each of the slaves
    public void setRSAInformation(String publicKey, String chiffre, String amountOfPrimes) throws IOException {
        System.out.println("\nMaster sent: Computing information");

        this.publicKey = publicKey;
        this.chiffre = chiffre;
        this.amountOfPrimes = amountOfPrimes;

        ArrayList<String> primes = readFromFile(amountOfPrimes);

        int lengthOfSubArray = primes.size() / this.NodeList.size();

        int i = 0;
        for (SlaveHandler slaveHandler : slaveHandlerList) {
            int startIndex = lengthOfSubArray * i;
            int endIndex;

            if (i == NodeList.size() - 1) {
                endIndex = primes.size();
            } else {
                endIndex = lengthOfSubArray * (i + 1);
            }

            // Contains: [0] = public key
            // Contains: [1] = start index
            // Contains: [2] = end index
            // Contains: [...] = primes
            ArrayList<String> rsaInformation = new ArrayList<>();
            rsaInformation.add(publicKey);
            rsaInformation.add(String.valueOf(startIndex));
            rsaInformation.add(String.valueOf(endIndex));
            rsaInformation.addAll(primes);

            slaveHandler.sendToSlave(rsaInformation);
            i++;
        }
        firstPointInTime = System.currentTimeMillis();
    }

    // Reads in the primes depending on amountOfPrimes
    public static ArrayList readFromFile(String amountOfPrimes) {
        ArrayList<String> content = new ArrayList<>();

        String filename = "resources/primes" + amountOfPrimes + ".txt";

        try {
            File file = new File(filename);
            Scanner myReader = new Scanner(file);

            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                content.add(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occured while reading the file");
            e.printStackTrace();
        }
        return content;
    }


    public void closeWorkingThread() throws InterruptedException {
        closeSlave = true;
    }

    // Sends found solution to master
    public void shareSolution(String p, String q) throws IOException {
        Message message = new Message();
        ArrayList<String> pq = new ArrayList<>();
        pq.add(p);
        pq.add(q);
        message.setPayload(pq);
        message.setType("RSA-SOLUTION");
        slaveMasterObjectOutputStream.writeObject(message);
    }

    // Master decrypts chiffre depending on p and q
    public String decrypt(String p, String q) {
        System.out.println("Time for decryption: " + (System.currentTimeMillis()-firstPointInTime) + "ms");
        RSAHelper helper = new RSAHelper();
        return helper.decrypt(p, q, chiffre);
    }

    // Distributes solved chiffre to the slaves
    public void distributeSolution(String chiffre) throws IOException {
        for (SlaveHandler slaveHandler : slaveHandlerList) {
            slaveHandler.sendRSASolution(chiffre);
        }
    }
}


