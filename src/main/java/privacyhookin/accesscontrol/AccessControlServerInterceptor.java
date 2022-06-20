package privacyhookin.accesscontrol;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * This interceptor gets the JWT from the metadata, verifies it and sets the client identifier
 * obtained from the token into the context. In order not to complicate the example with additional
 * checks (expiration date, issuer and etc.), it relies only on the signature of the token for
 * verification.
 */
public class AccessControlServerInterceptor implements ServerInterceptor {

  private JwtParser parser = Jwts.parser().setSigningKey(AccessControlUtils.JWT_SIGNING_KEY);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    String value = metadata.get(AccessControlUtils.AUTHORIZATION_METADATA_KEY);

    Status status = Status.OK;
    if (value == null) {
      status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
    } else if (!value.startsWith(AccessControlUtils.BEARER_TYPE)) {
      status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
    } else {
      Jws<Claims> claims = null;
      // remove authorization type prefix
      String token = value.substring(AccessControlUtils.BEARER_TYPE.length()).trim();
      try {
        // verify token signature and parse claims
        claims = parser.parseClaimsJws(token);
      } catch (JwtException e) {
        status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
      }
      if (claims != null) {
        // set client id into current context
        Context ctx = Context.current()
            .withValue(AccessControlUtils.CLIENT_ID_CONTEXT_KEY, claims.getBody().getSubject());
        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
      }
    }

    serverCall.close(status, new Metadata());
    return new ServerCall.Listener<ReqT>() {
      // noop
    };
  }

}
