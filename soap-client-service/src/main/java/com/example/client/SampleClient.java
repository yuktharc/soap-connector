
package com.example.client;

import com.yourorg.soap.SoapClientConfig;
import com.yourorg.soap.SoapConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SampleClient {

    @Autowired
    private SoapConnector soapConnector;

    @Bean
    public Jaxb2Marshaller myMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.example.client.generated");
        return marshaller;
    }

    public void callService() {
        SoapClientConfig config = new SoapClientConfig.Builder()
                .withUri("https://example.org/ws")
                .withMarshaller(myMarshaller())
                .withConnectTimeout(3000)
                .withReadTimeout(5000)
                .withRetryCount(2)
                .withXmlLoggingEnabled(true)
                .build();

        Object response = soapConnector.call(config, new Object(), Object.class, Map.of("Auth-Token", "ABC123"));
        System.out.println(response);
    }
}
