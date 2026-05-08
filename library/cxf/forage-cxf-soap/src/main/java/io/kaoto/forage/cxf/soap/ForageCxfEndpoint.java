package io.kaoto.forage.cxf.soap;

import javax.net.ssl.SSLContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForageCxfEndpoint extends CxfEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ForageCxfEndpoint.class);
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0");
    private String sslContextParametersBeanName;
    private final AtomicBoolean sslConfigured = new AtomicBoolean(false);
    private String cxfServletPath;
    private String runtimeName;

    public void setQuarkusCxfServletPath(String path) {
        this.cxfServletPath = path;
        this.runtimeName = "Quarkus";
    }

    public void setSpringBootCxfServletPath(String path) {
        this.cxfServletPath = path;
        this.runtimeName = "Spring Boot";
    }

    public void setSslContextParametersBeanName(String beanName) {
        this.sslContextParametersBeanName = beanName;
    }

    public String getSslContextParametersBeanName() {
        return sslContextParametersBeanName;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        adaptAddressForServletContainer();
        return super.createConsumer(processor);
    }

    private void adaptAddressForServletContainer() {
        if (cxfServletPath == null) {
            return;
        }

        String address = getAddress();
        if (address == null || address.startsWith("/")) {
            return;
        }

        URI uri;
        try {
            uri = new URI(address);
        } catch (URISyntaxException e) {
            return;
        }

        String host = uri.getHost();
        if (host == null || !LOCAL_HOSTS.contains(host.toLowerCase())) {
            return;
        }

        String servletPath = cxfServletPath;
        if (!servletPath.startsWith("/")) {
            servletPath = "/" + servletPath;
        }
        if (servletPath.endsWith("/")) {
            servletPath = servletPath.substring(0, servletPath.length() - 1);
        }

        String path = uri.getPath();
        String relativePath;
        if (path != null && (path.equals(servletPath) || path.startsWith(servletPath + "/"))) {
            relativePath = path.substring(servletPath.length());
            if (relativePath.isEmpty()) {
                relativePath = "/";
            }
        } else {
            relativePath = path != null ? path : "/";
        }

        LOG.warn(
                "Absolute CXF address '{}' detected on {} server endpoint; "
                        + "adapting to relative path '{}' (CXF servlet root: '{}')",
                address,
                runtimeName,
                relativePath,
                servletPath);

        setAddress(relativePath);
    }

    @Override
    public Bus getBus() {
        Bus bus = super.getBus();

        if (sslContextParametersBeanName != null && sslConfigured.compareAndSet(false, true)) {
            applySsl(bus);
        }

        return bus;
    }

    private void applySsl(Bus bus) {
        SSLContextParameters sslCtx = getCamelContext()
                .getRegistry()
                .lookupByNameAndType(sslContextParametersBeanName, SSLContextParameters.class);
        if (sslCtx == null) {
            LOG.warn("SSL context parameters bean '{}' not found in registry", sslContextParametersBeanName);
            return;
        }

        try {
            SSLContext sslContext = sslCtx.createSSLContext(getCamelContext());
            TLSClientParameters tlsParams = new TLSClientParameters();
            tlsParams.setSSLSocketFactory(sslContext.getSocketFactory());

            setSslContextParameters(sslCtx);

            HTTPConduitConfigurer existing = bus.getExtension(HTTPConduitConfigurer.class);
            HTTPConduitConfigurer tlsConfigurer = (name, address, conduit) -> conduit.setTlsClientParameters(tlsParams);
            HTTPConduitConfigurer chained = existing == null
                    ? tlsConfigurer
                    : (name, address, conduit) -> {
                        existing.configure(name, address, conduit);
                        tlsConfigurer.configure(name, address, conduit);
                    };
            bus.setExtension(chained, HTTPConduitConfigurer.class);
        } catch (Exception e) {
            LOG.warn("Could not configure CXF Bus with SSL context: {}", e.getMessage(), e);
        }
    }
}
