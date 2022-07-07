package delivery.client;

import clientside.AccessControlClientCredentials;
import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.common.Status;
import com.peng.gprc_hook_in.order.OrderRequest;
import com.peng.gprc_hook_in.order.OrderServiceGrpc;
import delivery.utils.ServicesParser;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import java.nio.file.Paths;

public class Client {
    private static ServicesParser servicesParser;

    private static final String clientId = "client";

    public Client() {
        StringBuilder stringBuilder = new StringBuilder();
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        String configFile = stringBuilder.append(cwd).append("/delivery/src/main/resources/services.json").toString();
        servicesParser = new ServicesParser(configFile);
    }

    public void test() {
        // TODO: benchmarking
        System.out.println("Order start");
        String keyPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/privateKeys/private_key_client.der";
        OrderRequest request = OrderRequest.newBuilder()
                .setName("Professor")
                .setSurname("Tai")
                .setAddress("Strasse des 17 Juni")
                .setMeal("Currywurst")
                .build();
        Channel channel = ManagedChannelBuilder.forAddress(
                        servicesParser.getHost("order"),
                        servicesParser.getPort("order"))
                .usePlaintext().build();
        OrderServiceGrpc.OrderServiceBlockingStub orderStub = OrderServiceGrpc
                .newBlockingStub(channel)
                .withCallCredentials(new AccessControlClientCredentials(clientId, "meal_purchase", keyPath));
        ResultResponse response = orderStub.orderMeal(request);
        if (response.getStatus() == Status.SUCCESS){
            System.out.println("Order completed and delivered successfully");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.test();
    }
}
