
package com.yourorg.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.soap.client.core.SoapFaultMessageResolver;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.io.IOException;
import java.util.Map;

public class SoapConnector {
    private static final Logger logger = LoggerFactory.getLogger(SoapConnector.class);

    public <T> T call(
            SoapClientConfig config,
            Object requestPayload,
            Class<T> responseClass,
            Map<String, String> soapHeaders
    ) {
        WebServiceTemplate wst = new WebServiceTemplate();
        wst.setMarshaller(config.getMarshaller());
        wst.setUnmarshaller(config.getMarshaller());
        wst.setDefaultUri(config.getUri());
        wst.setFaultMessageResolver(new SoapFaultMessageResolver());

        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        try {
            sender.setConnectionTimeout(config.getConnectTimeout());
            sender.setReadTimeout(config.getReadTimeout());
            wst.setMessageSender(sender);
        } catch (IOException e) {
            throw new RuntimeException("Error configuring HTTP timeouts", e);
        }

        if (config.isEnableXmlLogging()) {
            wst.setInterceptors(new ClientInterceptor[]{
                new LoggingInterceptor(),
                new HeaderInterceptor(soapHeaders)
            });
        } else {
            wst.setInterceptors(new ClientInterceptor[]{
                new HeaderInterceptor(soapHeaders)
            });
        }

        int retries = config.getRetryCount();
        while (retries-- > 0) {
            try {
                Object response = wst.marshalSendAndReceive(requestPayload, new SoapActionCallback(config.getUri()));
                return responseClass.cast(response);
            } catch (Exception ex) {
                logger.error("SOAP call failed. Retries left: {}", retries, ex);
                if (retries == 0) {
                    throw new RuntimeException("SOAP call failed after retries", ex);
                }
            }
        }
        throw new RuntimeException("SOAP call failed with no retries left");
    }

    static class HeaderInterceptor implements ClientInterceptor {
        private final Map<String, String> headers;

        public HeaderInterceptor(Map<String, String> headers) {
            this.headers = headers;
        }

        @Override public boolean handleRequest(MessageContext ctx) {
            if (headers != null && !headers.isEmpty()) {
                SoapHeader header = (SoapHeader) ctx.getRequest().getSoapHeader();
                headers.forEach((k, v) -> {
                    try {
                        header.addHeaderElement(new javax.xml.namespace.QName(k)).setText(v);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to add header: " + k, e);
                    }
                });
            }
            return true;
        }
        @Override public boolean handleResponse(MessageContext ctx) { return true; }
        @Override public boolean handleFault(MessageContext ctx) { return true; }
        @Override public void afterCompletion(MessageContext ctx, Exception ex) {}
    }

    static class LoggingInterceptor implements ClientInterceptor {
        @Override public boolean handleRequest(MessageContext ctx) { log(ctx.getRequest()); return true; }
        @Override public boolean handleResponse(MessageContext ctx) { log(ctx.getResponse()); return true; }
        @Override public boolean handleFault(MessageContext ctx) { log(ctx.getResponse()); return true; }
        @Override public void afterCompletion(MessageContext ctx, Exception ex) {}
        private void log(WebServiceMessage msg) {
            try {
                logger.info(msg.getPayloadSource().toString());
            } catch (Exception e) {
                logger.warn("Logging failed", e);
            }
        }
    }
}
