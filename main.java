import java.util.ArrayList;

public class main {
    public static void main(String[] args) throws InterruptedException {
        int amountSlaves = 1;
        int masterPort = 8005;
        ArrayList<Thread> threads = new ArrayList<>();
        System.out.println("Start main");

        MasterSlave runnableMaster = new MasterSlave("Master", masterPort);
        Thread newMaster = new Thread(runnableMaster);
        newMaster.start();
        threads.add(newMaster);

        MasterSlave runnableSlave = new MasterSlave("Slave", 8003, masterPort);
        Thread newSlave = new Thread(runnableSlave);
        newSlave.start();
        threads.add(newSlave);

        MasterSlave runnableSlave2 = new MasterSlave("Slave", 8008, masterPort);
        Thread newSlave2 = new Thread(runnableSlave2);
        newSlave2.start();
        threads.add(newSlave2);

//        Client runnableClient = new Client(8003, "localhost");
//        Thread newClient = new Thread(runnableClient);
//        newClient.start();
//        threads.add(newClient);

        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
            System.out.println("Thread no. " + (i + 1) + " has been joined");
        }
    }
}
