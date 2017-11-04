package scenario;

import com.commsignia.v2x.client.ITSApplication;
import com.commsignia.v2x.client.ITSEventAdapter;
import com.commsignia.v2x.client.MessageSet;
import com.commsignia.v2x.client.exception.ClientException;
import com.commsignia.v2x.client.model.*;
import communication.*;
import communication.Message.MessageCommon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

import static com.commsignia.v2x.utils.units.LengthUnit.M;

public class ObuScenario1 {

    public static final int DEFAULT_ITS_AID = 9;
    public static final String DEFAULT_TARGET_HOST = "192.168.1.92";
    public static final int DEFAULT_TARGET_PORT = 7942;
    public static final MessageSet DEFAULT_MESSAGE_SET = MessageSet.D;
    public static final int BTP_PORT = 1235;
    public static final int TX_POWER_DBM = 0;

    private static final int NAVIGATION_PORT = 11111;
    private static final int SOCKET_PORT = 15000;
    private static final String NAVIGATION_ADDRESS="localhost";
    private static ITSApplication itsApplication = null;
    private static final Object targetCircleLock = new Object();
    private static DestAreaCircle targetCircle;


    public static void main(String[] args) {
        List<String> route = new ArrayList<>();



        try {
            String targetHost = args.length > 0 ? args[0] : DEFAULT_TARGET_HOST;
            itsApplication = new ITSApplication(DEFAULT_ITS_AID, targetHost, DEFAULT_TARGET_PORT, DEFAULT_MESSAGE_SET);
            itsApplication.connect(10000);

            itsApplication.commands().registerBlocking();
            itsApplication.commands().setDeviceTime(System.currentTimeMillis() / 1000L);

            Queue<String> gbcQueue = new ConcurrentLinkedQueue<>();

            itsApplication.addEventListener(new ITSEventAdapter() {
                @Override
                public void onGnNotification(GNNotification notification) {
                    ByteBuffer buffer = ByteBuffer.wrap(notification.getData());

                    System.out.printf("GN GBC receive. GN address: %s  RSSI: %d dBm\n",
                            notification.getGNAddress(),
                            notification.getRssi()
                    );

                    String message = StandardCharsets.UTF_8.decode(buffer).toString();
                    System.out.println("Kapott OBU message: " + message );
                    CommandEnum command = FactoryMessage.getCommandType(message);
                    if(command == CommandEnum.REROUTE){
                        sendSocketMessage(NAVIGATION_ADDRESS, NAVIGATION_PORT, message);
                    }

                }
            });
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

            itsApplication.commands().ldmSubscribe(Collections.singletonList(
                    new LdmFilter().setObjectTypeFilter(LdmObjectType.STATION)
            ));
            itsApplication.commands().gnBindBlocking(BTPType.B, BTP_PORT);

            GbcClient gbcClient = new GbcClient(itsApplication,gbcQueue,BTP_PORT,targetCircle,targetCircleLock);
            gbcClient.start();
            SocketServer socketServer = new SocketServer(gbcQueue, SOCKET_PORT);
            socketServer.start();

            MessageCommon messageCommon = new MessageCommon(CommandEnum.ROUTE, "555");
            String messageJSON = FactoryMessage.convertToJson(messageCommon);
            sendSocketMessage(NAVIGATION_ADDRESS,NAVIGATION_PORT,messageJSON);




           /* Thread t = new Thread() {
                private int i = 0;


            };

            t.start();*/

            System.out.println("Press any key to terminate...");
            System.in.read();

            gbcClient.interrupt();
            socketServer.interrupt();
            //t.interrupt();

            itsApplication.commands().gnCloseBlocking(BTPType.B, BTP_PORT);

            itsApplication.commands().deregisterBlocking();


        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //15000
        /*ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Optional<MessageRoute> > socketServer = new SocketServer(15000);
        SocketClient sendMessage =
                new SocketClient("localhost",
                        11111, messageJSON);
        sendMessage.start();
        Future<Optional<MessageRoute>> value = executor.submit(socketServer);
        Optional<MessageRoute> messageRoute = Optional.empty();
        try {
            if(value.get().isPresent()) {
                messageRoute = Optional.of(value.get().get());
                System.out.println("The returned value is : " + value.get().get().getVehicleID());
                System.out.println("The returned value is : "+value.get().get().getRoute());
                route = value.get().get().getRoute();
            }else
                System.out.println("No value :(");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        executor.shutdown();

        if(messageRoute.isPresent()) {
            try {
                messageJSON = objectMapper.writeValueAsString(messageRoute.get());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            if (!messageJSON.isEmpty()) {
                SocketClient sendMessageRSU =
                        new SocketClient("localhost",
                                15001, messageJSON);
                sendMessageRSU.start();
            }
        }*/

        System.out.println("Vege");


    }

    private static void sendSocketMessage(String address, int port, String message){
        SocketClient sendMessage =
                new SocketClient(address,
                        port, message);
        sendMessage.start();
    }
}
