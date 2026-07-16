package io.kaoto.forage.springboot.common.jta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import io.kaoto.forage.core.jta.recovery.ForageRecoveryService;

/**
 * Stops the JVM-global Narayana recovery manager when the Spring application context closes.
 *
 * <p>Recovery helpers are registered lazily while Forage providers create XA-enabled
 * {@code ConnectionFactory}/{@code DataSource} beans (see {@link ForageRecoveryService}); this
 * bean is the matching end-of-life hook so no recovery thread outlives the application context.
 */
public class ForageRecoveryShutdownBean implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ForageRecoveryShutdownBean.class);

    @Override
    public void destroy() {
        log.debug("Shutting down the XA recovery service on application context close");
        ForageRecoveryService.getInstance().shutdown();
    }
}
