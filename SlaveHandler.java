import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class SlaveHandler implements Runnable {

    Socket s;
    private long pid;
    private long tid;
    private MasterSlave master;

    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    private boolean slaveAnsweredHeartbeat = true;

    public SlaveHandler(Socket s, MasterSlave master) {
        this.master = master;
        this.s = s;
    }

    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        try {
            OutputStream outputStream = s.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            this.objectOutputStream = objectOutputStream;

            InputStream inputStream = s.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            this.objectInputStream = objectInputStream;

            System.out.println("SlaveHandler " + pid + "-" + tid + " is ready");

            discoveryRequest(objectOutputStream, objectInputStream);

            while (true) {
                Message message = read(objectInputStream);
                if (message.getType().equals("HEARTBEAT-RESPONSE")) {
                    System.out.println("SlaveHandler " + pid + "-" + tid + " received: Heartbeat response ");
                    slaveAnsweredHeartbeat = true;
                } else {
//                    TextMessage receivedText = (TextMessage) message.getPayload();
//                    System.out.println("SlaveHandler " + pid + "-" + tid + " received: " + receivedText.getMessage());

                    message = queryMessage(message);
                    objectOutputStream.writeObject(message);
                    System.out.println("SlaveHandler " + pid + "-" + tid + " responded: " + ((TextMessage) message.getPayload()).getMessage());
                }

            }
        } catch (IOException e) {
            System.out.println("A SlaveHandler " + pid + "-" + tid + " error occured.");
            e.printStackTrace();
        }
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

    // Sends out discovery request to the slave and adds the response node to array in the master
    public void discoveryRequest(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) throws IOException {
        Message message = new Message();
        message.setType("DISCOVERY");

        objectOutputStream.writeObject(message);
        System.out.println("\nSlaveHandler " + pid + "-" + tid + " sent: Discovery request");
        message = read(objectInputStream);
        System.out.println("SlaveHandler " + pid + "-" + tid + " received: Discovery response");
        if (message.getType().equals("DISCOVERY-RESPONSE")) {
            this.master.addNode((Node) (message.getPayload()));
        }
    }

    // Returns message with text message as payload depending on the input type and payload
    public Message createMessage(String type, String payload) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        return message;
    }

    // Computes message depending on the type and payload of the message
    public Message queryMessage(Message message) throws IOException {

        // If message type is WRITE, the slaveHandler writes the payload into sockets.txt and create a response message with OK as payload
        if (message.getType().equals("WRITE")) {
            String text = ((TextMessage) message.getPayload()).getMessage();
            System.out.println("SlaveHandler does: WRITE");
            FileWriter fw = new FileWriter("sockets.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(text);
            bw.write(System.getProperty("line.separator"));
            bw.close();
            fw.close();
            return createMessage("READ", "OK");

        // If message type is READ, send and create a response message with the last x messages as payload depending on the received payload
        } else if (message.getType().equals("READ")) {
            String text = ((TextMessage) message.getPayload()).getMessage();
            System.out.println("SlaveHandler does: READ");
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
                for (int i = lastEntries.size(); i > (lastEntries.size() - Integer.parseInt(text)); i--) {
                    txt += lastEntries.get(i - 1);
                    txt += "; ";
                }
                return createMessage("READ", txt);
            } catch (FileNotFoundException e) {
                System.out.println("A client handle error occured.");
                e.printStackTrace();
            }

        // If message type equals RSA, start RSA process at the master and create a response message with confirmation as payload
        } else if (message.getType().equals("RSA")) {
            System.out.println("SlaveHandler does: RSA-INFORMATION");

            String[] arrayMessage = (String[]) message.getPayload();

            master.findRSASolution(arrayMessage[0], arrayMessage[1], arrayMessage[2]);
            return createMessage("RSA-RESPONSE", "Master received RSA information");

        // If message type equals RSA-SOLUTION, start the distribution process at the master and create a response message with confirmation as payload
        }else if (message.getType().equals("RSA-SOLUTION")){
            ArrayList<String> pqPrimes = (ArrayList<String>) message.getPayload();
            master.distributeSolution(master.decrypt(pqPrimes.get(0), pqPrimes.get(1)));
            return createMessage("RSA-RESPONSE", "Master received RSA solution");
        }
        return createMessage("ERROR", "No matching message type");
    }

    // Sends given ArrayList to connected slave
    public void sendToSlave(ArrayList<String> rsaInformation) throws IOException {
        Message message = new Message();
        message.setType("RSA-INFORMATION");
        message.setPayload(rsaInformation);
        objectOutputStream.writeObject(message);
        System.out.println("SlaveHandler " + pid + "-" + tid + " forwarded: RSA information");
    }

    // Sends given NodeList to connected slave
    public void sendNewList(ArrayList NodeList) throws IOException {
        Message message = new Message();
        message.setPayload(NodeList);
        message.setType("NEW LIST");

        String list = "";
        for (Node element : ((ArrayList<Node>) message.getPayload())) {
            list += element.getID() + ", ";
        }
        objectOutputStream.reset();
        System.out.println("SlaveHandler " + pid + "-" + tid + ": Sending updated Nodelist to Slave: " + list);
        objectOutputStream.writeObject(message);
    }


    public boolean getSlaveAnsweredHeartbeat() {
        return slaveAnsweredHeartbeat;
    }

    // Sends heartbeat request to connected slave
    public void heartBeat() throws IOException {
        Message message = new Message();
        message.setType("HEARTBEAT");
        slaveAnsweredHeartbeat = false;
        objectOutputStream.writeObject(message);
        System.out.println("\nSlaveHandler " + pid + "-" + tid + " sent: HEARTBEAT request");
    }

    // Sends RSA solution to connected slave
    public void sendRSASolution(String chiffreText) throws IOException {
        objectOutputStream.writeObject(createMessage("RSA-SOLUTION", chiffreText));
        System.out.println("\nSlaveHandler " + pid + "-" + tid + " sent: send RSA solution");
    }
}