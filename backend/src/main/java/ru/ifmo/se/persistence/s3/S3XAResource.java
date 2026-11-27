package ru.ifmo.se.persistence.s3;

import ru.ifmo.se.application.exception.StorageException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

class S3XAResource implements XAResource {
    private final S3PreparedStorage preparedStorage;
    private final S3PreparedStorage.PreparedUpload upload;
    private final Map<String, String> preparedKeysByXid = new ConcurrentHashMap<>();
    private final AtomicInteger transactionTimeoutSeconds = new AtomicInteger(0);

    S3XAResource(S3PreparedStorage preparedStorage, S3PreparedStorage.PreparedUpload upload) {
        this.preparedStorage = preparedStorage;
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
        try {
            String preparedKey = preparedStorage.stage(xid, upload);
            preparedKeysByXid.put(XidCodec.encode(xid), preparedKey);
            return XA_OK;
        } catch (StorageException e) {
            throw toXaException("prepare", e);
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String xidKey = XidCodec.encode(xid);
        if (onePhase && !preparedKeysByXid.containsKey(xidKey) && upload != null) {
            prepare(xid);
        }

        try {
            String preparedKey = preparedKeysByXid.get(xidKey);
            preparedStorage.promote(xid, preparedKey, upload);
            preparedKeysByXid.remove(xidKey);
        } catch (StorageException e) {
            throw toXaException("commit", e);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        String xidKey = XidCodec.encode(xid);
        String preparedKey = preparedKeysByXid.remove(xidKey);
        if (preparedKey == null && upload != null) {
            return;
        }

        try {
            preparedStorage.abort(xid, preparedKey);
        } catch (StorageException e) {
            throw toXaException("rollback", e);
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
            return preparedStorage.listPreparedXids();
        } catch (StorageException e) {
            throw toXaException("recover", e);
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

    private static XAException toXaException(String operation, StorageException cause) {
        XAException ex = new XAException("S3 XA " + operation + " failed: " + cause.getLocalizedMessage());
        ex.errorCode = XAException.XAER_RMERR;
        ex.initCause(cause);
        return ex;
    }
}
