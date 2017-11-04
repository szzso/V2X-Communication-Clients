package communication;

import com.commsignia.v2x.client.ITSApplication;
import com.commsignia.v2x.client.exception.ClientException;
import com.commsignia.v2x.client.model.DestAreaCircle;
import com.commsignia.v2x.client.model.GeonetSendData;

import java.nio.ByteBuffer;
import java.util.Queue;

public class GbcClient extends Thread {

    private ITSApplication itsApplication;
    private static DestAreaCircle targetCircle;
    private static Object targetCircleLock;
    private static int BTP_PORT;
    private Queue<String> queue;

    public GbcClient(ITSApplication itsApplication, Queue<String> queue, int BTP_PORT, DestAreaCircle targetCircle, Object targetCircleLock) {
        GbcClient.targetCircle = targetCircle;
        GbcClient.targetCircleLock = targetCircleLock;
        GbcClient.BTP_PORT = BTP_PORT;
        this.queue = queue;
        this.itsApplication = itsApplication;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {

            if (!queue.isEmpty()) {
                String messageJSON = queue.poll();

                try {
                    synchronized (targetCircleLock) {
                        if (targetCircle != null) {
                            ByteBuffer buffer = ByteBuffer.wrap(messageJSON.getBytes());

                            buffer.flip();
                            System.out.println("new message");
                            itsApplication.commands().gnSendBlocking(new GeonetSendData.Builder()
                                    .asBTPB()
                                    .asGBC()
                                    //.withTxPowerDbm(TX_POWER_DBM)
                                    .withDestinationAreaCircle(targetCircle)
                                    .withDestinationAddress("00:00:ff:ff:ff:ff:ff:ff")
                                    .withDestinationPort(BTP_PORT)
                                    //.withTrafficClass(false, false, 0)
                                    .withData(buffer.array())
                                    //.withDestinationAreaCircle(10, 20, 100, M)
                                    .build()
                            );
                            System.out.println("GN GBC send. Successful. BTP-B " + BTP_PORT + " Message: " + messageJSON);
                        }
                    }


                } catch (ClientException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

    }
}

