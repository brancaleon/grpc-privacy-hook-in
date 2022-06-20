package privacyhookin.accesscontrol;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.concurrent.Executor;

/**
 * CallCredentials implementation, which carries the JWT value that will be propagated to the
 * server in the request metadata with the "Authorization" key and the "Bearer" prefix.
 */
public class AccessControlJwtCredential extends CallCredentials {

  private final String subject;

  public AccessControlJwtCredential(String subject) {
    this.subject = subject;
  }

  @Override
  public void applyRequestMetadata(final RequestInfo requestInfo, final Executor executor,
      final MetadataApplier metadataApplier) {
    // Make a JWT compact serialized string.
    // This example omits setting the expiration, but a real application should do it.
    final String jwt =
        Jwts.builder()
            .setSubject(subject)
            .signWith(SignatureAlgorithm.HS256, AccessControlUtils.JWT_SIGNING_KEY)
            .compact();

    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          Metadata headers = new Metadata();
          headers.put(AccessControlUtils.AUTHORIZATION_METADATA_KEY,
              String.format("%s %s", AccessControlUtils.BEARER_TYPE, jwt));
          metadataApplier.apply(headers);
        } catch (Throwable e) {
          metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
        }
      }
    });
  }

  @Override
  public void thisUsesUnstableApi() {
    // noop
  }
}
