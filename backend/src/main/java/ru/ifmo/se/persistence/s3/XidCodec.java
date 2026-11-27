package ru.ifmo.se.persistence.s3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.transaction.xa.Xid;

final class XidCodec {
    private static final int PARTS_COUNT = 3;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private XidCodec() {
    }

    static String encode(Xid xid) {
        byte[] gtrid = xid.getGlobalTransactionId();
        byte[] bqual = xid.getBranchQualifier();
        return xid.getFormatId()
                + "."
                + ENCODER.encodeToString(gtrid != null ? gtrid : new byte[0])
                + "."
                + ENCODER.encodeToString(bqual != null ? bqual : new byte[0]);
    }

    static Optional<Xid> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        String[] parts = encoded.split("\\.", -1);
        if (parts.length != PARTS_COUNT) {
            return Optional.empty();
        }
        try {
            return Optional.of(new S3Xid(
                    Integer.parseInt(parts[0]),
                    DECODER.decode(parts[1]),
                    DECODER.decode(parts[2])
            ));
        } catch (IllegalArgumentException ignore) {
            return Optional.empty();
        }
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
