import java.io.IOException;
import java.util.TimerTask;

public class ConnectionChecker extends TimerTask {

    private MasterSlave master;

    public ConnectionChecker(MasterSlave master) {
        this.master = master;
    }

    public void run() {
        System.out.println("ConnectionChecker running");
        for(SlaveHandler sh: master.slaveHandlerList){
            try {
                if (sh.getSlaveAnsweredHeartbeat() == true){
                    System.out.println("ConnectionChecker sending Heartbeat");
                    sh.heartBeat();
                }else {
                    master.threads.get(sh).join();
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
