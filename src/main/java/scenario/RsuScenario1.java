package scenario;

import com.commsignia.v2x.client.ITSApplication;
import com.commsignia.v2x.client.ITSEventAdapter;
import com.commsignia.v2x.client.MessageSet;
import com.commsignia.v2x.client.exception.ClientException;
import com.commsignia.v2x.client.model.BTPType;
import com.commsignia.v2x.client.model.DestAreaCircle;
import com.commsignia.v2x.client.model.GNNotification;
import communication.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

import static com.commsignia.v2x.utils.units.LengthUnit.M;

public class RsuScenario1 {

    public static final int DEFAULT_ITS_AID = 9;
    public static final String DEFAULT_TARGET_HOST = "192.168.1.91";
    public static final int DEFAULT_TARGET_PORT = 7942;
    public static final MessageSet DEFAULT_MESSAGE_SET = MessageSet.D;
    public static final int BTP_PORT = 1235;
    public static final int TX_POWER_DBM = 0;

    private static final int TMC_PORT = 12222;
    private static final int SOCKET_PORT = 15000;
    private static final String TMC_ADDRESS = "localhost";

    private static ITSApplication itsApplication = null;
    private static final Object targetCircleLock = new Object();
    private static DestAreaCircle targetCircle;

    public static void main(String[] args) {


        try {
            String targetHost = args.length > 0 ? args[0] : DEFAULT_TARGET_HOST;
            itsApplication = new ITSApplication(DEFAULT_ITS_AID, targetHost, DEFAULT_TARGET_PORT, DEFAULT_MESSAGE_SET);
            itsApplication.connect(10000);

            itsApplication.commands().registerBlocking();
            itsApplication.commands().setDeviceTime(System.currentTimeMillis() / 1000L);

            Queue<String> gbcQueue = new ConcurrentLinkedQueue<>();
            targetCircle = new DestAreaCircle.Builder()
                    .withPosition(10, 20)
                    .withRadius(1000, M)
                    .build();

            itsApplication.addEventListener(new ITSEventAdapter() {
                @Override
                public void onGnNotification(GNNotification notification) {
                    ByteBuffer buffer = ByteBuffer.wrap(notification.getData());

                    System.out.printf("GN GBC receive. GN address: %s  RSSI: %d dBm\n",
                            notification.getGNAddress(),
                            notification.getRssi()
                    );

                    String message = StandardCharsets.UTF_8.decode(buffer).toString();
                    System.out.println("Kapott RSU message: " + message);
                    CommandEnum command = FactoryMessage.getCommandType(message);
                    if (command == CommandEnum.ROUTE) {
                        sendSocketMessage(TMC_ADDRESS, TMC_PORT, message);
                    }

                }
            });
            itsApplication.commands().gnBindBlocking(BTPType.B, BTP_PORT);

            GbcClient gbcClient = new GbcClient(itsApplication, gbcQueue, BTP_PORT, targetCircle, targetCircleLock);
            gbcClient.start();
            SocketServer socketServer = new SocketServer(gbcQueue, SOCKET_PORT);
            socketServer.start();

            System.out.println("Press any key to terminate...");
            System.in.read();

            gbcClient.interrupt();
            socketServer.interrupt();

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


        /*ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Optional<MessageRoute> > socketServer = new SocketServer(15001);
        Future<Optional<MessageRoute>> value = executor.submit(socketServer);
        Optional<MessageRoute> messageRoute = Optional.empty();
        try {
            if(value.get().isPresent()) {
                messageRoute = Optional.of(value.get().get());
                System.out.println("Message from : " + value.get().get().getVehicleID());
            }else
                System.out.println("No value :(");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        executor.shutdown();

        if(messageRoute.isPresent()) {
            ObjectMapper objectMapper = new ObjectMapper();
            String messageJSON = "";
            try {
                messageJSON = objectMapper.writeValueAsString(messageRoute.get());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            if (!messageJSON.isEmpty()) {
                SocketClient sendMessage =
                        new SocketClient("localhost",
                                12222, messageJSON);
                sendMessage.start();
            }*/
        // }
    }

    private static void sendSocketMessage(String address, int port, String message) {
        SocketClient sendMessage =
                new SocketClient(address,
                        port, message);
        sendMessage.start();
    }
}
