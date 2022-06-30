package privacyhookin.accesscontrol;

import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.Executor;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * CallCredentials implementation, which carries the JWT value that will be propagated to the
 * server in the request metadata with the "Authorization" key and the "Bearer" prefix.
 */
public class AccessControlClientCredentials extends CallCredentials {

  static public final String PURPOSES_FILE = Paths.get(".").toAbsolutePath().normalize() +
          "/src/main/java/privacyhookin/accesscontrol/purposes.json";
  static public final String BEARER_TYPE = "Bearer";

  static public final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
  static public final Metadata.Key<String> CLIENT_ID_METADATA_KEY = Metadata.Key.of("clientId", ASCII_STRING_MARSHALLER);
  static public final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

  private final String subject;
  private final String purpose;

  public AccessControlClientCredentials(String subject, String purpose) {
    this.subject = subject;
    this.purpose = purpose;
  }

  @Override
  public void applyRequestMetadata(final RequestInfo requestInfo, final Executor executor,
      final MetadataApplier metadataApplier) {
    final String jwt =
        Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .claim("purpose", purpose)
            .setSubject(subject)
            .setIssuedAt(Date.from(Instant.now())).setExpiration(Date.from(Instant.now().plus(8, ChronoUnit.HOURS)))
            .signWith(SignatureAlgorithm.RS512, getPrivateKey(this.subject))
            .compact();

    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          Metadata headers = new Metadata();
          headers.put(AccessControlClientCredentials.AUTHORIZATION_METADATA_KEY,
              String.format("%s %s", AccessControlClientCredentials.BEARER_TYPE, jwt));
          headers.put(AccessControlClientCredentials.CLIENT_ID_METADATA_KEY, subject);
          metadataApplier.apply(headers);
        } catch (Throwable e) {
          metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
        }
      }
    });
  }

  static public PublicKey getPublicKey(String clientId){
    byte[] publicKeyBytes = new AccessControlPurposesParser(PURPOSES_FILE).getKeyBytes("public_key", clientId);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
    KeyFactory kf = null;
    try {
      kf = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    try {
      return kf.generatePublic(spec);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  static public PrivateKey getPrivateKey(String clientId){
    byte[] privateKeyBytes = new AccessControlPurposesParser(PURPOSES_FILE).getKeyBytes("private_key", clientId);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
    KeyFactory kf = null;
    try {
      kf = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    try {
      return kf.generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void thisUsesUnstableApi() {
    // noop
  }
}
