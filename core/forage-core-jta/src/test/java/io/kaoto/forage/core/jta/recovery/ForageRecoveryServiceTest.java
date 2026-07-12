package io.kaoto.forage.core.jta.recovery;

import javax.transaction.xa.XAResource;

import java.nio.file.Path;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the JVM-global recovery manager lifecycle. Narayana state is a JVM singleton, so every
 * test restores a clean state through {@link ForageRecoveryService#shutdown()}.
 */
class ForageRecoveryServiceTest {

    private static final String PERIODIC_RECOVERY_THREAD = "Periodic Recovery";

    @TempDir
    static Path objectStoreDir;

    private final ForageRecoveryService service = ForageRecoveryService.getInstance();

    @BeforeAll
    static void useTemporaryObjectStore() {
        String dir = objectStoreDir.toAbsolutePath().toString();
        BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreDir(dir);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
                .setObjectStoreDir(dir);
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
                .setObjectStoreDir(dir);
    }

    @AfterEach
    void shutdownRecovery() {
        service.shutdown();
        awaitNoPeriodicRecoveryThread();
    }

    @Test
    void noRecoveryThreadWhenNothingRegistered() {
        assertThat(periodicRecoveryThreadRunning()).isFalse();
    }

    @Test
    void registerHelperStartsManagerOnce() {
        service.registerHelper("jms:a", noopHelper());
        assertThat(periodicRecoveryThreadRunning()).isTrue();

        service.registerHelper("jms:b", noopHelper());
        assertThat(periodicRecoveryThreadCount()).isEqualTo(1);
    }

    @Test
    void reRegisteringSameKeyReplacesPreviousHelper() {
        service.registerHelper("jms:a", noopHelper());
        service.registerHelper("jms:a", noopHelper());

        // a single deregistration must leave nothing behind: the manager stops
        service.deregisterHelpers("jms:a");
        service.stopIfNoRegistrations();
        awaitNoPeriodicRecoveryThread();
        assertThat(periodicRecoveryThreadRunning()).isFalse();
    }

    @Test
    void stopIfNoRegistrationsKeepsManagerWhileHelpersRemain() {
        service.registerHelper("jms:a", noopHelper());
        service.registerHelper("jdbc:b", noopHelper());

        service.deregisterHelpers("jms:a");
        service.stopIfNoRegistrations();
        assertThat(periodicRecoveryThreadRunning()).isTrue();

        service.deregisterHelpers("jdbc:b");
        service.stopIfNoRegistrations();
        awaitNoPeriodicRecoveryThread();
        assertThat(periodicRecoveryThreadRunning()).isFalse();
    }

    @Test
    void restartAfterShutdownWorks() {
        service.registerHelper("jms:a", noopHelper());
        service.shutdown();
        awaitNoPeriodicRecoveryThread();

        // must not hit Narayana's stale XARecoveryModule static: a fresh manager must start
        service.registerHelper("jms:b", noopHelper());
        assertThat(periodicRecoveryThreadRunning()).isTrue();
    }

    @Test
    void scopedRegistryTracksAndRemovesSameInstance() {
        XAResourceRecoveryRegistry registry = service.scopedRegistry("jdbc:ds");
        XAResourceRecovery recovery = () -> new XAResource[0];

        registry.addXAResourceRecovery(recovery);
        assertThat(periodicRecoveryThreadRunning()).isTrue();

        registry.removeXAResourceRecovery(recovery);
        service.stopIfNoRegistrations();
        awaitNoPeriodicRecoveryThread();
        assertThat(periodicRecoveryThreadRunning()).isFalse();
    }

    @Test
    void deregisteringUnknownKeyIsANoOp() {
        service.deregisterHelpers("unknown");
        assertThat(periodicRecoveryThreadRunning()).isFalse();
    }

    private static XAResourceRecoveryHelper noopHelper() {
        return new XAResourceRecoveryHelper() {
            @Override
            public boolean initialise(String param) {
                return true;
            }

            @Override
            public XAResource[] getXAResources() {
                return new XAResource[0];
            }
        };
    }

    private static boolean periodicRecoveryThreadRunning() {
        return periodicRecoveryThreadCount() > 0;
    }

    private static long periodicRecoveryThreadCount() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().contains(PERIODIC_RECOVERY_THREAD))
                .filter(Thread::isAlive)
                .count();
    }

    private static void awaitNoPeriodicRecoveryThread() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (periodicRecoveryThreadRunning() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
