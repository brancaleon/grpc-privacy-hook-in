package order;

import phi_access_control.Constant;
import phi_data_minimization.RequestInterceptor;
import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.common.Status;
import com.peng.gprc_hook_in.driver.DriverAssignmentRequest;
import com.peng.gprc_hook_in.driver.DriverServiceGrpc;
import com.peng.gprc_hook_in.order.OrderRequest;
import com.peng.gprc_hook_in.order.OrderServiceGrpc;
import com.peng.gprc_hook_in.restaurant.MealOrderRequest;
import com.peng.gprc_hook_in.restaurant.RestaurantServiceGrpc;
import com.peng.gprc_hook_in.routing.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.logging.Logger;

import static com.peng.gprc_hook_in.common.Status.SUCCESS;

public class OrderServer {

  private static final Logger logger = Logger.getLogger(OrderServer.class.getName());

  private Server server;
  private final int port;

  public OrderServer(int port) {
    this.port = port;
  }

  private void start() throws IOException {
    server = ServerBuilder.forPort(port)
        .addService(new OrderImpl())
        .intercept(new RequestInterceptor())  // add the JwtServerInterceptor
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
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }

    final OrderServer server = new OrderServer(port);
    server.start();
    server.blockUntilShutdown();
  }

  static class OrderImpl extends OrderServiceGrpc.OrderServiceImplBase {
    @Override
    public void orderMeal(OrderRequest req, StreamObserver<ResultResponse> responseObserver) {
      // TODO: fullfill the order
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
      // get client id added to context by interceptor
      String clientId = Constant.CLIENT_ID_CONTEXT_KEY.get();
      logger.info("Processing request from " + clientId);
      Status status = SUCCESS;
      // ResultResponse reply = ResultResponse.newBuilder().setStatus(status).setField(new Descriptors.FieldDescriptor, new int[5]).build();
      // responseObserver.onNext(reply);
      // responseObserver.onCompleted();
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
