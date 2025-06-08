
package com.yourorg.soap;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import java.util.List;

public class SoapClientConfig {
    private final String uri;
    private final Jaxb2Marshaller marshaller;
    private final List<ClientInterceptor> interceptors;
    private final int connectTimeout;
    private final int readTimeout;
    private final int retryCount;
    private final boolean enableXmlLogging;

    private SoapClientConfig(Builder builder) {
        this.uri = builder.uri;
        this.marshaller = builder.marshaller;
        this.interceptors = builder.interceptors;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.retryCount = builder.retryCount;
        this.enableXmlLogging = builder.enableXmlLogging;
    }

    public String getUri() { return uri; }
    public Jaxb2Marshaller getMarshaller() { return marshaller; }
    public List<ClientInterceptor> getInterceptors() { return interceptors; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getRetryCount() { return retryCount; }
    public boolean isEnableXmlLogging() { return enableXmlLogging; }

    public static class Builder {
        private String uri;
        private Jaxb2Marshaller marshaller;
        private List<ClientInterceptor> interceptors = List.of();
        private int connectTimeout = 5000;
        private int readTimeout = 5000;
        private int retryCount = 3;
        private boolean enableXmlLogging = false;

        public Builder withUri(String uri) { this.uri = uri; return this; }
        public Builder withMarshaller(Jaxb2Marshaller marshaller) { this.marshaller = marshaller; return this; }
        public Builder withInterceptors(List<ClientInterceptor> interceptors) { this.interceptors = interceptors; return this; }
        public Builder withConnectTimeout(int timeout) { this.connectTimeout = timeout; return this; }
        public Builder withReadTimeout(int timeout) { this.readTimeout = timeout; return this; }
        public Builder withRetryCount(int count) { this.retryCount = count; return this; }
        public Builder withXmlLoggingEnabled(boolean flag) { this.enableXmlLogging = flag; return this; }
        public SoapClientConfig build() { return new SoapClientConfig(this); }
    }
}
