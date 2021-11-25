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

    private boolean slaveAnsweredHeartbeat;

    public SlaveHandler(Socket s, MasterSlave master ) {
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
                }else{
                    TextMessage receivedText = (TextMessage) message.getPayload();
                    System.out.println("SlaveHandler " + pid + "-" + tid + " received: " + receivedText.getMessage());

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

    public Message read(ObjectInputStream ois) {
        Message ret = null;
        try {
            ret = (Message) ois.readObject();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return ret;
    }

    public void discoveryRequest(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) throws IOException {
        Message message = new Message();
        message.setType("DISCOVERY");

        objectOutputStream.writeObject(message);
        System.out.println("\nSlaveHandler " + pid + "-" + tid + ": Discovery request send");
        message = read(objectInputStream);
        System.out.println("SlaveHandler " + pid + "-" + tid + ": Discovery response received");
        if (message.getType().equals("DISCOVERY-RESPONSE")) {
            this.master.addNode((Node) (message.getPayload()));
        }
    }

    public Message sendMessage(String type, String payload) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        return message;
    }

    public Message queryMessage(Message message) throws IOException {
        String lastEntry = "";
        if (message.getType().equals("WRITE")) {
            String text = ((TextMessage) message.getPayload()).getMessage();
            System.out.println("SlaveHandler does: WRITE");
            FileWriter fw = new FileWriter("sockets.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(text);
            bw.write(System.getProperty("line.separator"));
            bw.close();
            fw.close();
            return sendMessage("READ", "OK");
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
                return sendMessage("READ", txt);
            } catch (FileNotFoundException e) {
                System.out.println("A client handle error occured.");
                e.printStackTrace();
            }
        }
        return sendMessage("ERROR", "No matching message type");
    }

    public void sendNewList(ArrayList NodeList) throws IOException {
        Message message = new Message();
        message.setPayload(NodeList);
        message.setType("NEW LIST");

        String list = "";
        for(Node element: ((ArrayList<Node>) message.getPayload())){
            list += element.getID() + ", ";
        }
        objectOutputStream.reset();
        System.out.println("SlaveHandler " + pid + "-" + tid + ": Sending updated Nodelist to Slave: " + list);
        objectOutputStream.writeObject(message);

    }

    public boolean getSlaveAnsweredHeartbeat() {
        return slaveAnsweredHeartbeat;
    }

    public void heartBeat() throws IOException {
        Message message = new Message();
        message.setType("HEARTBEAT");
        slaveAnsweredHeartbeat = false;
        objectOutputStream.writeObject(message);
        System.out.println("\nSlaveHandler " + pid + "-" + tid + ": HEARTBEAT request send");
    }
}