package client;

import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.order.OrderRequest;
import com.peng.gprc_hook_in.order.OrderServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import privacyhookin.accesscontrol.AccessControlJwtCredential;
import utils.ServicesParser;

import java.nio.file.Paths;

public class Client {
  private static ServicesParser servicesParser;

  private static final String clientId = "client";

  public Client() {
    StringBuilder stringBuilder = new StringBuilder();
    String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    String configFile = stringBuilder.append(cwd).append("/src/main/java/order/services.json").toString();
    servicesParser = new ServicesParser(configFile);
  }

  public void test(){
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
            .withCallCredentials(new AccessControlJwtCredential(clientId, "meal_purchase"));
    ResultResponse response = orderStub.orderMeal(request);
  }

  public static void main(String[] args) throws Exception {
    Client client = new Client();
    client.test();
  }
}
