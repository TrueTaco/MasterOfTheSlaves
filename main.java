import java.util.ArrayList;

public class main {
    public static void main(String[] args) throws InterruptedException {
        int amountSlaves = 1;
        ArrayList<Thread> threads = new ArrayList<>();
        System.out.println("Start main");

        MasterSlave runnableMaster = new MasterSlave("Master");
        Thread newMaster = new Thread(runnableMaster);
        newMaster.start();
        threads.add(newMaster);

        for(int i = 0; i < amountSlaves; i++) {
            MasterSlave runnableSlave = new MasterSlave("Slave");
            Thread newSlave = new Thread(runnableSlave);
            newSlave.start();
            threads.add(newSlave);
        }

        Client runnableClient = new Client(8005,"localhost");
        Thread newClient = new Thread(runnableClient);
        newClient.start();
        threads.add(newClient);

        for(int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
            System.out.println("Thread no. " + (i+1) + " has been joined");
        }
    }
}
