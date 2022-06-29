package privacyhookin.accesscontrol;

import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

public class Authorization {

    private Status errorStatus = null;
    private String purpose = null;
    private String subject = null;

    public Authorization(Metadata metadata) {
        String value = metadata.get(AccessControlUtils.AUTHORIZATION_METADATA_KEY);
        if (value == null) {
            errorStatus = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!value.startsWith(AccessControlUtils.BEARER_TYPE)) {
            errorStatus = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            // remove authorization type prefix
            String token = value.substring(AccessControlUtils.BEARER_TYPE.length()).trim();
            try {
                JwtParser parser = Jwts.parser().setSigningKey(
                        AccessControlUtils.getPublicKey(metadata.get(AccessControlUtils.CLIENT_ID_METADATA_KEY))
                );
                // verify token signature and parse claims
                Jws<Claims> claims = parser.parseClaimsJws(token);
                subject = claims.getBody().getSubject();
                purpose = (String) claims.getBody().get("purpose");
            } catch (Exception e) {
                errorStatus = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            }
        }
    }

    public String getPurposeOrNull() {
        return purpose;
    }

    public String getSubjectOrNull() {
        return subject;
    }

    public Status getErrorStatus() {
        return errorStatus;
    }
}
