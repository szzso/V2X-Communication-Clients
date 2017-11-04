package scenario;

import com.commsignia.v2x.client.ITSApplication;
import com.commsignia.v2x.client.ITSEventAdapter;
import com.commsignia.v2x.client.MessageSet;
import com.commsignia.v2x.client.exception.ClientException;
import com.commsignia.v2x.client.model.*;
import com.commsignia.v2x.client.model.dev.FacilityModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import static com.commsignia.v2x.utils.units.LengthUnit.M;

public class GNSHBExample {

    public static final int DEFAULT_ITS_AID = 6;
    public static final String DEFAULT_TARGET_HOST = "192.168.1.92";
    public static final int DEFAULT_TARGET_PORT = 7942;
    public static final MessageSet DEFAULT_MESSAGE_SET = MessageSet.D;
    public static final int BTP_PORT = 1234;
    public static final int TX_POWER_DBM = 20;

    private static ITSApplication itsApplication = null;

    private static final Object targetCircleLock = new Object();
    private static DestAreaCircle targetCircle;

    public static void main(String[] args) {
        try {
            String targetHost = args.length > 0 ? args[0] :  DEFAULT_TARGET_HOST;

            itsApplication = new ITSApplication(DEFAULT_ITS_AID, targetHost, DEFAULT_TARGET_PORT, DEFAULT_MESSAGE_SET);

            itsApplication.connect(10000);

            itsApplication.commands().registerBlocking();

            itsApplication.commands().setDeviceTime(System.currentTimeMillis() / 1000L);
/*
            //itsApplication.setFacilityModuleStatus(FacilityModule.BSM, false);
            itsApplication.addEventListener(new ITSEventAdapter() {
                @Override
                public void onGnNotification(GNNotification notification) {
                    ByteBuffer buffer = ByteBuffer.wrap(notification.getData());
                    System.out.printf("GN SHB receive. GN address: %s Sequence number: %d RSSI: %d dBm\n",
                            notification.getGNAddress(),
                            buffer.getInt(),
                            notification.getRssi()
                    );
                }
            });
            // update destination circle as station moves
            itsApplication.addEventListener(new ITSEventAdapter() {
                @Override
                public void onLdmNotificationReceived(LdmObject ldmObject) {
                    if (ldmObject.isLocal()) {
                        synchronized (targetCircleLock) {
                            targetCircle = new DestAreaCircle.Builder()
                                    .withPosition(ldmObject.getLatitude(), ldmObject.getLongitude())
                                    .withRadius(1000, M)
                                    .build();
                        }
                    }
                }
            });
            itsApplication.ldmSubscribe(Collections.singletonList(
                    new LdmFilter().setObjectTypeFilter(LdmObjectType.STATION)
            ));
*/
            itsApplication.commands().gnBindBlocking(BTPType.B, BTP_PORT);

            Thread t = new Thread() {
                private int i = 0;

                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        try {
                            targetCircle = new DestAreaCircle.Builder()
                                    .withPosition(10, 20)
                                    .withRadius(1000, M)
                                    .build();
                            //synchronized (targetCircleLock) {
                                if (targetCircle != null) {
                                    ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / 8);
                                    buffer.putInt(i);
                                    buffer.flip();

                                    itsApplication.commands().gnSendBlocking(new GeonetSendData.Builder()
                                            .asBTPB()
                                            .asSHB()
                                            .withTxPowerDbm(TX_POWER_DBM)
                                            .withDestinationAreaCircle(targetCircle)
                                            .withDestinationAddress("00:00:ff:ff:ff:ff:ff:ff")
                                            .withDestinationPort(BTP_PORT)
                                            .withTrafficClass(false, false, 0)
                                            .withData(buffer.array())
                                            .build()
                                    );
                                    System.out.println("GN SHB send. Successful. BTP-B " + BTP_PORT + " Sequence number " + i);

                                    i++;
                                }
                            //}
                        } catch (ClientException e) {
                            e.printStackTrace();
                            return;
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            };

            t.start();

            System.out.println("Press any key to terminate...");
            System.in.read();

            t.interrupt();

            itsApplication.commands().gnCloseBlocking(BTPType.B, BTP_PORT);

            itsApplication.commands().deregisterBlocking();
        } catch (IOException | InterruptedException | ClientException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            if (itsApplication != null) {
                itsApplication.shutdown();
            }
        }
    }

}