package delivery.restaurant;

import accesscontrol.AccessControlServerInterceptor;
import clientside.AccessControlClientCredentials;
import com.peng.gprc_hook_in.common.Driver;
import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.driver.DriverCheckRequest;
import com.peng.gprc_hook_in.driver.DriverServiceGrpc;
import com.peng.gprc_hook_in.order.OrderRequest;
import com.peng.gprc_hook_in.restaurant.CollectMealRequest;
import com.peng.gprc_hook_in.restaurant.RestaurantServiceGrpc;
import dataminimization.DataMinimizerInterceptor;
import delivery.utils.ServicesParser;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.peng.gprc_hook_in.common.Status.SUCCESS;

public class RestaurantServer {

    private static final Logger logger = Logger.getLogger(RestaurantServer.class.getName());
    private static ServicesParser servicesParser;
    private static final String clientId = "restaurant";

    private Server server;
    private final int port;

    public RestaurantServer() {
        StringBuilder stringBuilder = new StringBuilder();
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        String configFile = stringBuilder.append(cwd).append("/delivery/src/main/resources/services.json").toString();
        servicesParser = new ServicesParser(configFile);
        this.port = servicesParser.getPort(clientId);
    }

    private void start() throws IOException {
        String configPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/config.json";
        server = ServerBuilder.forPort(this.port)
                .addService(new RestaurantImpl())
                .intercept(new DataMinimizerInterceptor(configPath))
                .intercept(new AccessControlServerInterceptor(configPath))
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RestaurantServer.this.stop();
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
        final RestaurantServer server = new RestaurantServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class RestaurantImpl extends RestaurantServiceGrpc.RestaurantServiceImplBase {
        @Override
        public void cookMeal(OrderRequest request, StreamObserver<ResultResponse> responseObserver) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ResultResponse reply = ResultResponse.newBuilder().setStatus(SUCCESS).addMessages("Meal is ready").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void collectMeal(CollectMealRequest request, StreamObserver<ResultResponse> responseObserver) {
            String privateKeyPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/privateKeys/private_key_restaurant.der";
            Driver driver = request.getDriver();
            OrderRequest orderRequest = request.getOrderRequest();
            DriverCheckRequest driverRequest = DriverCheckRequest.newBuilder()
                    .setDriver(driver)
                    .setOrderRequest(orderRequest).build();
            Channel channel = ManagedChannelBuilder
                    .forAddress(servicesParser.getHost("driver"),
                            servicesParser.getPort("driver"))
                    .usePlaintext().build();
            DriverServiceGrpc.DriverServiceBlockingStub driverStub = DriverServiceGrpc
                    .newBlockingStub(channel)
                    .withCallCredentials(new AccessControlClientCredentials(clientId, "meal_collection", privateKeyPath));
            ResultResponse response = driverStub.checkDriverId(driverRequest);
            ResultResponse reply = null;
            if (response.getStatus() == SUCCESS) {
                reply = ResultResponse.newBuilder()
                        .setStatus(SUCCESS)
                        .addMessages("Meal collected by the driver").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

    }
}
