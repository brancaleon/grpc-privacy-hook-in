package delivery.routing;

import accesscontrol.AccessControlServerInterceptor;
import clientside.AccessControlClientCredentials;
import com.peng.gprc_hook_in.common.Driver;
import com.peng.gprc_hook_in.common.Position;
import com.peng.gprc_hook_in.driver.AvailableDriversResponse;
import com.peng.gprc_hook_in.driver.DriverListRequest;
import com.peng.gprc_hook_in.driver.DriverServiceGrpc;
import com.peng.gprc_hook_in.routing.DeliveryAddress;
import com.peng.gprc_hook_in.routing.RouteResponse;
import com.peng.gprc_hook_in.routing.RoutingRequest;
import com.peng.gprc_hook_in.routing.RoutingServiceGrpc;
import dataminimization.DataMinimizerInterceptor;
import delivery.utils.ServicesParser;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class RoutingServer {

    private static final Logger logger = Logger.getLogger(RoutingServer.class.getName());
    private static ServicesParser servicesParser;
    private static final String clientId = "routing";

    private Server server;
    private final int port;

    public RoutingServer() {
        StringBuilder stringBuilder = new StringBuilder();
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        String configFile = stringBuilder.append(cwd).append("/delivery/src/main/resources/services.json").toString();
        servicesParser = new ServicesParser(configFile);
        this.port = servicesParser.getPort(clientId);
    }

    private void start() throws IOException {
        String configPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/config.json";
        server = ServerBuilder.forPort(this.port)
                .addService(new RoutingImpl())
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
                RoutingServer.this.stop();
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
        final RoutingServer server = new RoutingServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class RoutingImpl extends RoutingServiceGrpc.RoutingServiceImplBase {

        @Override
        public void computeRoute(RoutingRequest request, StreamObserver<RouteResponse> responseObserver) {
            String privateKeyPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/privateKeys/private_key_routing.der";
            // TODO: choose the closest driver and compute the route for him
            //  Retrieve list of available drivers
            Channel channel = ManagedChannelBuilder
                    .forAddress(servicesParser.getHost("driver"),
                            servicesParser.getPort("driver"))
                    .usePlaintext().build();
            DriverServiceGrpc.DriverServiceBlockingStub driverStub = DriverServiceGrpc
                    .newBlockingStub(channel)
                    .withCallCredentials(new AccessControlClientCredentials(clientId, "route_computation", privateKeyPath));
            DriverListRequest driverListRequest = DriverListRequest.newBuilder().build();
            AvailableDriversResponse drivers = driverStub.getAvailableDrivers(driverListRequest);
            DeliveryAddress deliveryAddress = request.getAddress();
            Driver chosenDriver = this.findClosestDriver(drivers, deliveryAddress);
            RouteResponse reply = this.findRoute(chosenDriver.getPosition(), deliveryAddress);
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        public Driver findClosestDriver(AvailableDriversResponse drivers, DeliveryAddress deliveryAddress) {
            return drivers.getDrivers(0);
        }

        public RouteResponse findRoute(Position position, DeliveryAddress deliveryAddress) {
            return RouteResponse.newBuilder()
                    .addRoute(Position.newBuilder().setLatitude(1).setLatitude(3))
                    .addRoute(Position.newBuilder().setLatitude(5).setLatitude(8))
                    .addRoute(Position.newBuilder().setLatitude(4).setLatitude(3)).build();
        }

    }
}
