import java.util.ArrayList;

public class main {
    public static void main(String[] args) throws InterruptedException {
        int amountSlaves = 2;
        ArrayList<Thread> threads = new ArrayList<>();
        System.out.println("Start main");

        MasterSlave runnableMaster = new MasterSlave("Master",8005);
        Thread newMaster = new Thread(runnableMaster);
        newMaster.start();
        threads.add(newMaster);

        MasterSlave runnableSlave = new MasterSlave("Slave",8003,8005);
        Thread newSlave = new Thread(runnableSlave);
        newSlave.start();
        threads.add(newSlave);


        Client runnableClient = new Client(8003,"localhost");
        Thread newClient = new Thread(runnableClient);
        newClient.start();
        threads.add(newClient);

        for(int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
            System.out.println("Thread no. " + (i+1) + " has been joined");
        }
    }
}
