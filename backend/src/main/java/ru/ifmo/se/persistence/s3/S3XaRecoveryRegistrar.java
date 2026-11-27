package ru.ifmo.se.persistence.s3;

import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.xa.XAResource;

@ApplicationScoped
public class S3XaRecoveryRegistrar {
    @Inject
    private S3PreparedStorage preparedStorage;

    private final AtomicReference<XARecoveryModule> recoveryModuleRef = new AtomicReference<>();
    private final AtomicReference<XAResourceRecoveryHelper> helperRef = new AtomicReference<>();

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object init) {
        XARecoveryModule module = XARecoveryModule.getRegisteredXARecoveryModule();
        if (module != null) {
            XAResourceRecoveryHelper helper = new RecoveryHelper(preparedStorage);
            module.addXAResourceRecoveryHelper(helper);
            recoveryModuleRef.set(module);
            helperRef.set(helper);
        }
    }

    @PreDestroy
    public void onShutdown() {
        XARecoveryModule module = recoveryModuleRef.getAndSet(null);
        XAResourceRecoveryHelper helper = helperRef.getAndSet(null);
        if (module != null && helper != null) {
            module.removeXAResourceRecoveryHelper(helper);
        }
    }

    private record RecoveryHelper(S3PreparedStorage preparedStorage) implements XAResourceRecoveryHelper {
        @Override
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public boolean initialise(String properties) {
            return true;
        }

        @Override
        public XAResource[] getXAResources() {
            return new XAResource[] {new S3XAResource(preparedStorage, null)};
        }
    }
}
