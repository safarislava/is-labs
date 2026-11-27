package ru.ifmo.se.persistence.s3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

final class S3XidMapper {
    static final String PREPARED_PREFIX = "prepared/";
    static final String META_FINAL_KEY = "final-key";
    private static final int XID_PARTS_COUNT = 3;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private S3XidMapper() {
    }

    static String encodeXid(Xid xid) {
        byte[] gtrid = xid.getGlobalTransactionId();
        byte[] bqual = xid.getBranchQualifier();
        return xid.getFormatId()
                + "."
                + ENCODER.encodeToString(gtrid != null ? gtrid : new byte[0])
                + "."
                + ENCODER.encodeToString(bqual != null ? bqual : new byte[0]);
    }

    static String buildXidPrefix(String xidKey) {
        return PREPARED_PREFIX + xidKey + "/";
    }

    static Xid[] decodeXids(List<String> keys) {
        return keys.stream()
                .map(S3XidMapper::extractXidSegment)
                .filter(Objects::nonNull)
                .distinct()
                .map(S3XidMapper::decodeXid)
                .filter(Objects::nonNull)
                .toArray(Xid[]::new);
    }

    private static String extractXidSegment(String key) {
        if (key == null || !key.startsWith(PREPARED_PREFIX)) {
            return null;
        }
        int end = key.indexOf('/', PREPARED_PREFIX.length());
        return end > PREPARED_PREFIX.length() ? key.substring(PREPARED_PREFIX.length(), end) : null;
    }

    private static Xid decodeXid(String encoded) {
        String[] parts = encoded.split("\\.", -1);
        if (parts.length != XID_PARTS_COUNT) {
            return null;
        }
        try {
            return new S3Xid(
                    Integer.parseInt(parts[0]),
                    DECODER.decode(parts[1]),
                    DECODER.decode(parts[2])
            );
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    static XAException rmError(String operation, Throwable cause) {
        XAException ex = new XAException("S3 XA " + operation + " failed: " + cause.getLocalizedMessage());
        ex.errorCode = XAException.XAER_RMERR;
        ex.initCause(cause);
        return ex;
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class S3Xid implements Xid {
        private final int formatId;
        private final byte[] globalTransactionId;
        private final byte[] branchQualifier;

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Xid that
                    && formatId == that.getFormatId()
                    && Arrays.equals(globalTransactionId, that.getGlobalTransactionId())
                    && Arrays.equals(branchQualifier, that.getBranchQualifier());
        }

        @Override
        public int hashCode() {
            return 31 * (31 * Integer.hashCode(formatId) + Arrays.hashCode(globalTransactionId))
                    + Arrays.hashCode(branchQualifier);
        }
    }
}
