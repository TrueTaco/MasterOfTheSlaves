import java.util.ArrayList;
import java.util.TimerTask;

public class ConnectionChecker extends TimerTask {

    private MasterSlave master;

    public ConnectionChecker(MasterSlave master) {
        this.master = master;
    }

    // Instructs all slaveHandlers to do a heartbeat
    public void run() {
        ArrayList<SlaveHandler> unresponsiveSlaveHandlers = new ArrayList<>();

        if (master.slaveHandlerList.size() <= 0) return;

        System.out.print("\nSending HEARTBEAT requests");
        System.out.println(": " + master.slaveHandlerList.size() + " Slaves active");
        for (SlaveHandler sh : master.slaveHandlerList) {
            if (sh.getSlaveAnsweredHeartbeat() >= 3) {
                sh.heartBeat();
            } else {
                unresponsiveSlaveHandlers.add(sh);
            }

        }

        // Removes slaveHandler if slave did not answer in time
        for (SlaveHandler sh : unresponsiveSlaveHandlers) {
            try {
                master.threads.get(sh).join();
                master.slaveHandlerList.remove(sh);
                master.threads.remove(sh);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
