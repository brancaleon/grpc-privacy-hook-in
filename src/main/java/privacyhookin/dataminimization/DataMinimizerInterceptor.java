package privacyhookin.dataminimization;

import com.google.protobuf.Message;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import privacyhookin.accesscontrol.Authorization;

import java.nio.file.Paths;

public class DataMinimizerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Authorization authorization = new Authorization(metadata);
        if (authorization.getPurposeOrNull() == null) {
            serverCall.close(authorization.getErrorStatus(), new Metadata());
            return new ServerCall.Listener<ReqT>() {
                // noop
            };
        }
        ServerCall.Listener listener = serverCallHandler.startCall(serverCall, metadata);
        return new SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onMessage(ReqT req) {
                String configPath = Paths.get(".").toAbsolutePath().normalize()
                        + "/src/main/java/privacyhookin/dataminimization/minimizations.json";
                if (req instanceof Message) {
                    req = (ReqT) new DataMinimizer(configPath).minimize((Message) req, authorization.getPurposeOrNull());
                }
                super.onMessage(req);
            }
        };
    }
}