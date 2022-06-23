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
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import static privacyhookin.accesscontrol.AccessControlUtils.PURPOSES_FILE;

/**
 * This interceptor gets the JWT from the metadata, verifies it and sets the client identifier
 * obtained from the token into the context. In order not to complicate the example with additional
 * checks (expiration date, issuer and etc.), it relies only on the signature of the token for
 * verification.
 */
public class AccessControlServerInterceptor implements ServerInterceptor {
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
      // remove authorization type prefix
      String token = value.substring(AccessControlUtils.BEARER_TYPE.length()).trim();
      Jws<Claims> claims = null;
      try {
        JwtParser parser = Jwts.parser().setSigningKey(
                AccessControlUtils.getPublicKey(metadata.get(AccessControlUtils.CLIENT_ID_METADATA_KEY))
        );
        // verify token signature and parse claims
        claims = parser.parseClaimsJws(token);
        String subject = claims.getBody().getSubject();
        String purpose = (String) claims.getBody().get("purpose");
        boolean clientAllowedForPurpose = new AccessControlPurposesParser(PURPOSES_FILE).isAllowedPurpose(purpose, subject);
        if (!clientAllowedForPurpose){
          throw new Exception("Client unauthorized for this purpose");
        }
        // set client id into current context
        Context ctx = Context.current()
                .withValue(AccessControlUtils.CLIENT_ID_CONTEXT_KEY, subject);
        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
      } catch (Exception e) {
        status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
      }

    }

    serverCall.close(status, new Metadata());
    return new ServerCall.Listener<ReqT>() {
      // noop
    };
  }

}
