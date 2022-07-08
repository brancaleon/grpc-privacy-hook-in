package delivery.order;

import accesscontrol.AccessControlServerInterceptor;
import clientside.AccessControlClientCredentials;
import com.peng.gprc_hook_in.common.Driver;
import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.driver.DriverAssignmentRequest;
import com.peng.gprc_hook_in.driver.DriverServiceGrpc;
import com.peng.gprc_hook_in.order.OrderRequest;
import com.peng.gprc_hook_in.order.OrderServiceGrpc;
import com.peng.gprc_hook_in.restaurant.RestaurantServiceGrpc;
import com.peng.gprc_hook_in.routing.RouteResponse;
import com.peng.gprc_hook_in.routing.RoutingServiceGrpc;
import dataminimization.DataMinimizerInterceptor;
import delivery.driver.DriverServer;
import delivery.utils.ServicesParser;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.peng.gprc_hook_in.common.Status.SUCCESS;

public class OrderServer {

    private static final String PRIVATE_KEY_PATH = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/privateKeys/private_key_order.der";
    private static final Logger logger = Logger.getLogger(OrderServer.class.getName());
    private static ServicesParser servicesParser;
    private static final String clientId = "order";

    private Server server;
    private final int port;

    public OrderServer() {
        StringBuilder stringBuilder = new StringBuilder();
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        String configFile = stringBuilder.append(cwd).append("/delivery/src/main/resources/services.json").toString();
        servicesParser = new ServicesParser(configFile);
        this.port = servicesParser.getPort(clientId);
    }

    private void start() throws IOException {
        String configPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/config.json";
        server = ServerBuilder.forPort(this.port)
                .addService(new OrderImpl())
                .intercept(DataMinimizerInterceptor.newBuilder(configPath).build())
                .intercept(new AccessControlServerInterceptor(configPath))
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
        final OrderServer server = new OrderServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class OrderImpl extends OrderServiceGrpc.OrderServiceImplBase {

        @Override
        public void orderMeal(OrderRequest orderRequest, StreamObserver<ResultResponse> responseObserver) {
            // TODO: test entire application
            int orderId = 1;
//            System.out.println("Name: " + request.getName()); // Test output for minimization
//            System.out.println("Surname: " + request.getSurname());
            String meal = orderRequest.getMeal();
//             1. Send meal info to restaurant for cooking
            ResultResponse mealReady = this.SendMealInfo(orderRequest);
            // 2. Find route through RoutingService
            RouteResponse routeInfo = this.FindRoute(orderRequest);
            Driver driver = routeInfo.getChosenDriver();
            // 3. Assign delivery
            ResultResponse deliveryAssigned = this.AssignDelivery(orderRequest, driver);
            // 4. Receive confirmation from restaurant
            // 5. Receive confirmation from driver
            ResultResponse reply = ResultResponse.newBuilder().setStatus(SUCCESS).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        public ResultResponse SendMealInfo(OrderRequest orderRequest) {
            Channel channel = ManagedChannelBuilder
                    .forAddress(servicesParser.getHost("restaurant"),
                            servicesParser.getPort("restaurant"))
                    .usePlaintext().build();
            RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantStub = RestaurantServiceGrpc
                    .newBlockingStub(channel)
                    .withCallCredentials(new AccessControlClientCredentials(clientId, "meal_cooking", PRIVATE_KEY_PATH));
            return restaurantStub.cookMeal(orderRequest);
        }

        public RouteResponse FindRoute(OrderRequest orderRequest) {
            Channel channel = ManagedChannelBuilder
                    .forAddress(servicesParser.getHost("routing"),
                            servicesParser.getPort("routing"))
                    .usePlaintext().build();
            RoutingServiceGrpc.RoutingServiceBlockingStub routingStub = RoutingServiceGrpc
                    .newBlockingStub(channel)
                    .withCallCredentials(new AccessControlClientCredentials(clientId, "route_computation", PRIVATE_KEY_PATH));
            return routingStub.computeRoute(orderRequest);
        }

        public ResultResponse AssignDelivery(OrderRequest orderRequest, Driver driver) {
            DriverAssignmentRequest request = DriverAssignmentRequest.newBuilder()
                    .setOrderRequest(orderRequest)
                    .setDriver(driver).build();
            Channel channel = ManagedChannelBuilder
                    .forAddress(servicesParser.getHost("driver"),
                            servicesParser.getPort("driver"))
                    .usePlaintext().build();
            DriverServiceGrpc.DriverServiceBlockingStub driverStub = DriverServiceGrpc
                    .newBlockingStub(channel)
                    .withCallCredentials(new AccessControlClientCredentials(clientId, "delivery_assignment", PRIVATE_KEY_PATH));
            return driverStub.assignDriver(request);
        }

    }
}
