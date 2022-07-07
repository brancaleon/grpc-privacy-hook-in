package dataminimization;

import accesscontrol.Authorization;
import accesscontrol.ConfigParser;
import com.google.protobuf.Message;
import io.grpc.*;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;

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
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {
            @Override
            public void sendMessage(RespT message) {
                if (message instanceof Message) {
                    message = (RespT) minimizer.minimize((Message) message, authorization.getPurposeOrNull());
                }
                super.sendMessage(message);
            }
        }, metadata);
        return new SimpleForwardingServerCallListener<>(listener) {
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