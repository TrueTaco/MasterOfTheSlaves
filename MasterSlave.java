import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.ArrayList;

public class MasterSlave implements Runnable{

    private long pid;
    private long tid;

    private static final int maxIncomingClients = 100;
    private String type;
    private int slavePort;
    private int masterPort;
    private int masterConnectionPort;

    public MasterSlave(String type,int masterPort){
        this.type = type;
        if(type.equals("Master")){
            this.masterPort = masterPort;
        }else {
            throw new java.lang.Error("This constructor is used for Master");
        }
    }

    public MasterSlave(String type,int slavePort, int masterConnectionPort){
        this.type = type;
        if (type.equals("Slave")){
            this.slavePort = slavePort;
            this.masterConnectionPort = masterConnectionPort;
        }else {
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
        pid =  ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        if (type.equals("Slave")){
            try {
                // Client Socket
                ServerSocket clientServerSocket = new ServerSocket(slavePort, 100);
                Socket clientSocket = clientServerSocket.accept();

                OutputStream slaveClientOutputStream = clientSocket.getOutputStream();
                ObjectOutputStream slaveClientObjectOutputStream = new ObjectOutputStream(slaveClientOutputStream);

                InputStream slaveClientInputStream = clientSocket.getInputStream();
                ObjectInputStream slaveClientObjectInputStream = new ObjectInputStream(slaveClientInputStream);

                // Slave Socket
                Socket slaveServerSocket = initialiseClient( masterConnectionPort,"localhost");

                OutputStream slaveMasterOutputStream = slaveServerSocket.getOutputStream();
                ObjectOutputStream slaveMasterObjectOutputStream = new ObjectOutputStream(slaveMasterOutputStream);

                InputStream slaveMasterInputStream = slaveServerSocket.getInputStream();
                ObjectInputStream slaveMasterObjectInputStream = new ObjectInputStream(slaveMasterInputStream);

                System.out.println("Slave " + pid + "-" + tid + " is ready");


                while (true) {
                    Message message = read(slaveClientObjectInputStream);
                    slaveMasterObjectOutputStream.writeObject(message);
                    System.out.println("Slave: Sending message to Master");

                    message = read(slaveMasterObjectInputStream);
                    slaveClientObjectOutputStream.writeObject(message);
                    System.out.println("Slave: Sending message to Client");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(type.equals("Master")){
            try {
                ServerSocket masterServerSocket = new ServerSocket(masterPort, 100);
                Socket MasterSocket = masterServerSocket.accept();

                OutputStream masterSlaveOutputStream = MasterSocket.getOutputStream();
                ObjectOutputStream masterSlaveObjectOutputStream = new ObjectOutputStream(masterSlaveOutputStream);

                InputStream masterSlaveInputStream = MasterSocket.getInputStream();
                ObjectInputStream masterSlaveObjectInputStream = new ObjectInputStream(masterSlaveInputStream);

                System.out.println("Master " + pid + "-" + tid + " is ready");

                while (true){
                    Message message = read(masterSlaveObjectInputStream);
                    masterSlaveObjectOutputStream.writeObject(queryMessage(message));
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

    public Message queryMessage(Message message) throws IOException {
        String lastEntry = "";
        String text = ((TextMessage) message.getPayload()).getMessage();
        if (message.getType().equals("WRITE")){
            System.out.println("Master: WRITE received");
            FileWriter fw = new FileWriter("sockets.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(text);
            bw.write(System.getProperty("line.separator"));
            bw.close();
            fw.close();
            return sendMessage("READ", "OK");
        }else if (message.getType().equals("READ")){
            System.out.println("Master: READ received");
            try {
                File myObj = new File("sockets.txt");
                Scanner myReader = new Scanner(myObj);
                ArrayList<String> lastEntries = new ArrayList<String>();
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    lastEntries.add(data);
                }
                myReader.close();
                String txt = "";
                for(int i = lastEntries.size();i > (Integer.parseInt(text)); i--){
                    txt += lastEntries.get(i-1);
                }
                return sendMessage("READ", txt);
            } catch (FileNotFoundException e) {
                System.out.println("A client handle error occured.");
                e.printStackTrace();
            }
        }
        return sendMessage("ERROR", "No matching message type");
    }

    public Message sendMessage(String type, String payload){
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        return message;
    }
}


