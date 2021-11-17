import java.io.*;
import java.net.Socket;

public class SlaveHandler implements Runnable {

    Socket s;
    private long pid;
    private long tid;

    public SlaveHandler(Socket s) {
        this.s = s;
    }

    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        try {
            OutputStream outputStream = s.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            InputStream inputStream = s.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            System.out.println("SlaveHandler " + pid + "-" + tid + " is ready");

            discoveryRequest(objectOutputStream, objectInputStream);

            while (true) {
                Message message = read(objectInputStream);
                TextMessage receivedText = (TextMessage) message.getPayload();
                System.out.println("SlaveHandler " + pid + "-" + tid + " received: " + receivedText.getMessage());
            }
        } catch (IOException e) {
            System.out.println("A SlaveHandler error occured.");
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
//            NodeList.add((Node) (message.getPayload()));
        }
    }

}