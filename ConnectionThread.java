import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionThread implements Runnable {

    ServerSocket ss;
    MasterSlave father;
    ObjectOutputStream objectOutputStream;
    private long pid;
    private long tid;

    public ConnectionThread(ServerSocket ss, MasterSlave father) {
        this.ss = ss;
        this.father = father;
    }

    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        try {
            Socket clientSocket = ss.accept();

            OutputStream outputStream = clientSocket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            this.objectOutputStream = objectOutputStream;

            InputStream inputStream = clientSocket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            System.out.println("ConnectionThread " + pid + "-" + tid + " is ready");

            while (true) {
                Message message = read(objectInputStream);
                System.out.println("ConnectionThread " + pid + "-" + tid + ": Calling function in Slave for forwarding");
                father.forward(message);
            }
        } catch (IOException e) {
            System.out.println("A ConnectionThread error occured.");
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

    public void forward(Message message) throws IOException {
        // FORWARDING TO CLIENT
        if (objectOutputStream != null){
            this.objectOutputStream.writeObject(message);
            System.out.println("ConnectionThread " + pid + "-" + tid + ": Forwarding message to Client");
        }
    }
}