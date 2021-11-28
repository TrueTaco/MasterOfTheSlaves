import java.io.IOException;
import java.util.TimerTask;

public class ConnectionChecker extends TimerTask {

    private MasterSlave master;

    public ConnectionChecker(MasterSlave master) {
        this.master = master;
    }

    // Instructs the slaveHandlers to do a heartbeat
    public void run() {
        System.out.print("Sending HEARTBEAT requests");
        System.out.println(": " + master.slaveHandlerList.size() + " Slaves active");
        for(SlaveHandler sh: master.slaveHandlerList){
            try {
                if (sh.getSlaveAnsweredHeartbeat() == true){
                    sh.heartBeat();
                }else {
                    master.threads.get(sh).join();
                    master.slaveHandlerList.remove(sh);
                    master.threads.remove(sh);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
