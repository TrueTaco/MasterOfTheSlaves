import java.util.ArrayList;

public class main {
    public static void main(String[] args) throws InterruptedException {
        int amountSlaves = 3;
        int masterPort = 8000;
        ArrayList<Thread> threads = new ArrayList<>();
        System.out.println("Start main");

        MasterSlave runnableMaster = new MasterSlave("Master", masterPort);
        Thread newMaster = new Thread(runnableMaster);
        newMaster.start();
        threads.add(newMaster);
        Thread.sleep(100);

        for (int i = 0; i < amountSlaves; i++) {
            MasterSlave newRunnableSlave = new MasterSlave("Slave", masterPort + i + 1, masterPort, "localhost");
            Thread newSlave = new Thread(newRunnableSlave);
            newSlave.start();
            newSlave.setName("Slave " + i);
            threads.add(newSlave);
            Thread.sleep(100);
        }

        Thread.sleep(3000);
        Client runnableClient = new Client(masterPort + 1, "localhost");
        Thread newClient = new Thread(runnableClient);
        newClient.start();
        threads.add(newClient);

        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
            System.out.println("Thread no. " + (i + 1) + " has been joined");
        }
    }
}
