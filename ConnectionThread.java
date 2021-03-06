import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionThread implements Runnable {

    public ServerSocket ss;
    public MasterSlave father;
    public ObjectOutputStream objectOutputStream;
    public ObjectInputStream objectInputStream;
    public Socket clientSocket;
    public boolean connectedToClient = false;
    private long pid;
    private long tid;

    public ConnectionThread(ServerSocket ss, MasterSlave father) {
        this.ss = ss;
        this.father = father;
    }

    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        while (true) {
            try {
                // Wait for client to connect
                Socket clientSocket = ss.accept();
                connectedToClient = true;
                this.clientSocket = clientSocket;

                OutputStream outputStream = clientSocket.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                this.objectOutputStream = objectOutputStream;

                InputStream inputStream = clientSocket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                this.objectInputStream = objectInputStream;

                System.out.println("ConnectionThread " + pid + "-" + tid + " is ready");

                // Forward all messages from client to master
                while (true) {
                    if (objectInputStream.read() != -1) return;

                    Message message = (Message) objectInputStream.readObject();

                    System.out.println("ConnectionThread " + pid + "-" + tid + ": Calling function in Slave for forwarding");
                    father.forward(message);

                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("ConnectionThread lost connection to client");
                connectedToClient = false;
            }
        }
    }

    // Forwards message to client
    public void forward(Message message) throws IOException {
        if (objectOutputStream != null && connectedToClient) {
            System.out.println("ConnectionThread " + pid + "-" + tid + ": Forwarding message to Client");
            this.objectOutputStream.writeObject(message);
        }
    }
}