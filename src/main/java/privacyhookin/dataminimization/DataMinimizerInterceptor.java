package privacyhookin.dataminimization;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class DataMinimizerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {
        ServerCall.Listener listener = serverCallHandler.startCall(serverCall, metadata);
        return new SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override public void onMessage(ReqT req) {
                req = new DataMinimizer().minimize(req);
                super.onMessage(req);
            }
        };
    }
}