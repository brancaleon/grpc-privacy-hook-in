package clientside;

import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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

    static public final String BEARER_TYPE = "Bearer";

    static public final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    static public final Metadata.Key<String> CLIENT_ID_METADATA_KEY = Metadata.Key.of("clientId", ASCII_STRING_MARSHALLER);
    static public final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private final String subject;
    private final String purpose;
    private final String privateKeyPath;

    public AccessControlClientCredentials(String subject, String purpose, String privateKeyPath) {
        this.subject = subject;
        this.purpose = purpose;
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public void applyRequestMetadata(final RequestInfo requestInfo, final Executor executor, final MetadataApplier metadataApplier) {
        final String jwt =
                Jwts.builder()
                        .setId(UUID.randomUUID().toString())
                        .claim("purpose", purpose)
                        .setSubject(subject)
                        .setIssuedAt(Date.from(Instant.now())).setExpiration(Date.from(Instant.now().plus(8, ChronoUnit.HOURS)))
                        .signWith(SignatureAlgorithm.RS512, getPrivateKey())
                        .compact();

        executor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(AccessControlClientCredentials.AUTHORIZATION_METADATA_KEY,
                        String.format("%s %s", AccessControlClientCredentials.BEARER_TYPE, jwt));
                headers.put(AccessControlClientCredentials.CLIENT_ID_METADATA_KEY, subject);
                metadataApplier.apply(headers);
            } catch (Throwable e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }

    private PrivateKey getPrivateKey() {
        byte[] privateKeyBytes;
        try {
            privateKeyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void thisUsesUnstableApi() {
        // noop
    }
}
