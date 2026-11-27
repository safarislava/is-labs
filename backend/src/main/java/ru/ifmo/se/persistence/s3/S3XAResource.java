package ru.ifmo.se.persistence.s3;

import ru.ifmo.se.application.exception.StorageException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

class S3XAResource implements XAResource {
    private final S3ObjectClient objectClient;
    private final Upload upload;
    private final Map<String, String> preparedKeysByXid = new ConcurrentHashMap<>();
    private final AtomicInteger transactionTimeoutSeconds = new AtomicInteger(0);

    S3XAResource(S3ObjectClient objectClient, Upload upload) {
        this.objectClient = objectClient;
        this.upload = upload;
    }

    @Override
    public void start(Xid xid, int flags) {
    }

    @Override
    public void end(Xid xid, int flags) {
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        if (upload == null) {
            throw new XAException(XAException.XAER_PROTO);
        }
        String xidKey = S3XidMapper.encodeXid(xid);
        String preparedKey = S3XidMapper.buildXidPrefix(xidKey) + upload.fileName();
        try {
            objectClient.putAndVerify(
                    preparedKey,
                    upload.content(),
                    upload.contentType(),
                    Map.of(S3XidMapper.META_FINAL_KEY, upload.finalKey())
            );
            preparedKeysByXid.put(xidKey, preparedKey);
            return XA_OK;
        } catch (StorageException e) {
            objectClient.deleteQuietly(preparedKey);
            throw S3XidMapper.rmError("prepare", e);
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String xidKey = S3XidMapper.encodeXid(xid);
        if (onePhase && !preparedKeysByXid.containsKey(xidKey) && upload != null) {
            prepare(xid);
        }

        try {
            String preparedKey = preparedKeysByXid.get(xidKey);
            if (preparedKey == null) {
                preparedKey = objectClient.findFirstKey(S3XidMapper.buildXidPrefix(xidKey));
            }
            if (preparedKey == null) {
                return;
            }
            objectClient.promote(preparedKey, upload);
            preparedKeysByXid.remove(xidKey);
        } catch (StorageException e) {
            throw S3XidMapper.rmError("commit", e);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        String xidKey = S3XidMapper.encodeXid(xid);
        String preparedKey = preparedKeysByXid.remove(xidKey);
        if (preparedKey == null && upload != null) {
            return;
        }

        try {
            if (preparedKey != null) {
                objectClient.delete(preparedKey);
            } else {
                objectClient.deleteByPrefix(S3XidMapper.buildXidPrefix(xidKey));
            }
        } catch (StorageException e) {
            throw S3XidMapper.rmError("rollback", e);
        }
    }

    @Override
    public void forget(Xid xid) throws XAException {
        rollback(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        if ((flag & TMSTARTRSCAN) == 0) {
            return new Xid[0];
        }
        try {
            return S3XidMapper.decodeXids(objectClient.listKeys(S3XidMapper.PREPARED_PREFIX));
        } catch (StorageException e) {
            throw S3XidMapper.rmError("recover", e);
        }
    }

    @Override
    public boolean isSameRM(XAResource xares) {
        return equals(xares);
    }

    @Override
    public int getTransactionTimeout() {
        return transactionTimeoutSeconds.get();
    }

    @Override
    public boolean setTransactionTimeout(int seconds) {
        if (seconds < 0) {
            return false;
        }
        transactionTimeoutSeconds.set(seconds);
        return true;
    }

    record Upload(String fileName, String finalKey, byte[] content, String contentType) {
        Upload {
            content = content.clone();
        }
    }
}
