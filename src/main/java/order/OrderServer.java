package order;

import privacyhookin.accesscontrol.AccessControlJwtCredential;
import privacyhookin.accesscontrol.AccessControlUtils;
import privacyhookin.dataminimization.DataMinimizerInterceptor;
import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.driver.DriverAssignmentRequest;
import com.peng.gprc_hook_in.driver.DriverServiceGrpc;
import com.peng.gprc_hook_in.order.OrderRequest;
import com.peng.gprc_hook_in.order.OrderServiceGrpc;
import com.peng.gprc_hook_in.restaurant.MealOrderRequest;
import com.peng.gprc_hook_in.restaurant.RestaurantServiceGrpc;
import com.peng.gprc_hook_in.routing.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import utils.ServicesParser;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import static com.peng.gprc_hook_in.common.Status.SUCCESS;

public class OrderServer {

  private static final Logger logger = Logger.getLogger(OrderServer.class.getName());
  private final CallCredentials callCredentials;

  private Server server;
  private final String host;
  private final int port;

  public OrderServer() {
    StringBuilder stringBuilder = new StringBuilder();
    String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    stringBuilder.append(cwd);
    stringBuilder.append("/src/main/java/order/services.json");
    String configFile = stringBuilder.toString();
    Map servicesMap = ServicesParser.parse(configFile);
    String clientId = "client";
    this.callCredentials = new AccessControlJwtCredential(clientId);
    this.host = "localhost";
    this.port = 50001;
  }

  private void start() throws IOException {
    server = ServerBuilder.forPort(port)
        .addService(new OrderImpl())
        .intercept(new DataMinimizerInterceptor())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        OrderServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    int port = 50051; // default
    final OrderServer server = new OrderServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class OrderImpl extends OrderServiceGrpc.OrderServiceImplBase {
    @Override
    public void orderMeal(OrderRequest req, StreamObserver<ResultResponse> responseObserver) {
      // TODO: write order to db and get generated order id
      int orderId = 1;
      String meal = req.getMeal();
      // 1. Send meal info to restaurant for cooking
      ResultResponse mealReady = this.SendMealInfo(orderId, meal);
      // 2. Find route through RoutingService
      RouteResponse routeInfo = this.FindRoute(req.getAddress());
      String driverId = routeInfo.getChosenDriver().getId();
      // 3. Assign delivery
      ResultResponse deliveryAssigned = this.AssignDelivery(orderId, driverId);
      // 4. Receive confirmation from restaurant
      // 5. Receive confirmation from driver
      // 6. Finalize order
      // TODO: set order as completed
      // get client id added to context by interceptor
      String clientId = AccessControlUtils.CLIENT_ID_CONTEXT_KEY.get();
      logger.info("Processing request from " + clientId);
      ResultResponse reply = ResultResponse.newBuilder().setStatus(SUCCESS).setMessages(0, "OK").build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    public ResultResponse SendMealInfo(int orderId, String meal){
      MealOrderRequest request = MealOrderRequest.newBuilder().setId(orderId).setMeal(meal).build();
      Channel channel = ManagedChannelBuilder.forAddress("localhost", 50000).usePlaintext().build();
      RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantStub = RestaurantServiceGrpc.newBlockingStub(channel);
      return restaurantStub.cookMeal(request);
    }

    public RouteResponse FindRoute(String address) {
      DeliveryAddress deliveryAddress = DeliveryAddress.newBuilder().setAddress(address).build();
      RoutingRequest request = RoutingRequest.newBuilder().setAddress(deliveryAddress).build();
      Channel channel = ManagedChannelBuilder.forAddress("localhost", 50000).usePlaintext().build();
      RoutingServiceGrpc.RoutingServiceBlockingStub routingStub = RoutingServiceGrpc.newBlockingStub(channel);
      return routingStub.computeRoute(request);
    }

    public ResultResponse AssignDelivery(int orderId, String driverId){
      DriverAssignmentRequest request = DriverAssignmentRequest.newBuilder().setOrderId(orderId).setDriverId(driverId).build();
      Channel channel = ManagedChannelBuilder.forAddress("localhost", 50000).usePlaintext().build();
      DriverServiceGrpc.DriverServiceBlockingStub driverStub = DriverServiceGrpc.newBlockingStub(channel);
      return driverStub.assignDriver(request);
    }

  }
}
