package dataminimization;

import accesscontrol.Authorization;
import accesscontrol.ConfigParser;
import com.google.protobuf.Message;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class DataMinimizerInterceptor implements ServerInterceptor {

    private final DataMinimizer minimizer;
    private final ConfigParser config;

    public DataMinimizerInterceptor(String configPath) {
        config = new ConfigParser(configPath);
        minimizer = new DataMinimizer(config);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Authorization authorization = new Authorization(metadata, config);
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
                if (req instanceof Message) {
                    req = (ReqT) minimizer.minimize((Message) req, authorization.getPurposeOrNull());
                }
                super.onMessage(req);
            }
        };
    }


    public DataMinimizerInterceptor defineMinimizationFunction(String name, MinimizationFunction function) {
        minimizer.defineMinimizationFunction(name, function);
        return this;
    }
}