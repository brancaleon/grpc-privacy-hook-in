package delivery.driver;

import accesscontrol.AccessControlServerInterceptor;
import com.peng.gprc_hook_in.common.Driver;
import com.peng.gprc_hook_in.common.Position;
import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.driver.*;
import com.peng.gprc_hook_in.order.OrderRequest;
import dataminimization.DataMinimizerInterceptor;
import delivery.utils.ServicesParser;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.peng.gprc_hook_in.common.Status.ERROR;
import static com.peng.gprc_hook_in.common.Status.SUCCESS;

public class DriverServer {

    private static final Logger logger = Logger.getLogger(DriverServer.class.getName());
    private static final String clientId = "driver";

    private Server server;
    private final int port;

    public DriverServer() {
        StringBuilder stringBuilder = new StringBuilder();
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        String configFile = stringBuilder.append(cwd).append("/delivery/src/main/resources/services.json").toString();
        ServicesParser servicesParser = new ServicesParser(configFile);
        this.port = servicesParser.getPort(clientId);
    }

    private void start() throws IOException {
        String configPath = Paths.get(".").toAbsolutePath().normalize() + "/delivery/src/main/resources/config.json";
        server = ServerBuilder.forPort(this.port)
                .addService(new DriverImpl())
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
                DriverServer.this.stop();
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
        final DriverServer server = new DriverServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class DriverImpl extends DriverServiceGrpc.DriverServiceImplBase {

        @Override
        public void getAvailableDrivers(DriverListRequest request, StreamObserver<AvailableDriversResponse> responseObserver) {
            AvailableDriversResponse reply = AvailableDriversResponse.newBuilder().addDrivers(
                        Driver.newBuilder()
                                .setId("1")
                                .setName("Dr")
                                .setSurname("Grunewald")
                                .setPosition(Position.newBuilder()
                                        .setLatitude((float) 52.5125)
                                        .setLongitude((float) 13.3269)
                                .build())
                    ).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void assignDriver(DriverAssignmentRequest request, StreamObserver<ResultResponse> responseObserver) {
            // link the driver to the order on db
            Driver driver = request.getDriver();
            OrderRequest orderRequest = request.getOrderRequest();
            ResultResponse reply = ResultResponse.newBuilder().setStatus(SUCCESS).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void checkDriverId(DriverCheckRequest request, StreamObserver<ResultResponse> responseObserver) {
            String orderDriverId = "1";
            ResultResponse reply = null;
            if (request.getDriver().getId().equals(orderDriverId)) {
                reply = ResultResponse.newBuilder().setStatus(SUCCESS).build();
            } else {
                reply = ResultResponse.newBuilder()
                        .setStatus(ERROR)
                        .addMessages("He is not the legitimate driver, do not give him any meals!").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

    }
}
