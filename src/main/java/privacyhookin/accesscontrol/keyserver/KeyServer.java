package privacyhookin.accesscontrol.keyserver;

import com.peng.gprc_hook_in.common.ResultResponse;
import com.peng.gprc_hook_in.keyserver.KeyServerServiceGrpc;
import com.peng.gprc_hook_in.keyserver.TokenVerificationRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import utils.ServicesParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.peng.gprc_hook_in.common.Status.SUCCESS;

public class KeyServer {

  private static final Logger logger = Logger.getLogger(KeyServer.class.getName());
  private static ServicesParser servicesParser;
  private static final String clientId = "keyserver";

  private Server server;
  private final int port;

  public KeyServer() {
    StringBuilder stringBuilder = new StringBuilder();
    String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    String configFile = stringBuilder.append(cwd).append("/src/main/java/order/services.json").toString();
    servicesParser = new ServicesParser(configFile);
    this.port = servicesParser.getPort(clientId);
  }

  private void start() throws IOException {
    server = ServerBuilder.forPort(this.port)
        .addService(new KeyServerImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        KeyServer.this.stop();
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
    final KeyServer server = new KeyServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class KeyServerImpl extends KeyServerServiceGrpc.KeyServerServiceImplBase {

    @Override
    public void verifyToken(TokenVerificationRequest request, StreamObserver<ResultResponse> responseObserver) {
      super.verifyToken(request, responseObserver);
    }
  }
}
