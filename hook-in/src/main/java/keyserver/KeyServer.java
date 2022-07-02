package keyserver;

import com.google.protobuf.ByteString;
import com.peng.gprc_hook_in.keyserver.KeyServerServiceGrpc;
import com.peng.gprc_hook_in.keyserver.PublicKeyRequest;
import com.peng.gprc_hook_in.keyserver.PublicKeyResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class KeyServer {

    private static final Logger logger = Logger.getLogger(KeyServer.class.getName());

    private Server server;
    private final int port;

    public KeyServer(int port) {
        this.port = port;
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(this.port)
                .addService(new KeyServerImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            KeyServer.this.stop();
            System.err.println("*** server shut down");
        }));
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
        if (args.length != 1) {
            throw new IllegalArgumentException("First (and only) argument has to define the port");
        }
        final KeyServer server = new KeyServer(Integer.parseInt(args[0]));
        server.start();
        server.blockUntilShutdown();
    }

    static class KeyServerImpl extends KeyServerServiceGrpc.KeyServerServiceImplBase {

        @Override
        public void getPublicKey(PublicKeyRequest request, StreamObserver<PublicKeyResponse> responseObserver) {
            String client = request.getClient();
            String keyPath = Paths.get(".").toAbsolutePath().normalize()
                    + String.format("/hook-in/src/main/resources/publicKeys/public_key_%s.der", client);
            byte[] key;
            try {
                key = Files.readAllBytes(Paths.get(keyPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PublicKeyResponse response = PublicKeyResponse.newBuilder().setKey(ByteString.copyFrom(key)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
