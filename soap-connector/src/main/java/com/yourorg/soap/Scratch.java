package com.yourorg.soap;

public class Scratch {
}

import jakarta.xml.bind.JAXBElement;
import java.lang.reflect.Method;

public class JaxbWrapperUtil {

    @SuppressWarnings("unchecked")
    public static JAXBElement<?> wrap(Object request) {
        try {
            // Get the package where the request class is located
            String packageName = request.getClass().getPackageName();
            Class<?> objectFactoryClass = Class.forName(packageName + ".ObjectFactory");
            Object factoryInstance = objectFactoryClass.getDeclaredConstructor().newInstance();

            for (Method method : objectFactoryClass.getMethods()) {
                if (method.getName().startsWith("create")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(request.getClass())) {

                    Object result = method.invoke(factoryInstance, request);
                    if (result instanceof JAXBElement<?>) {
                        return (JAXBElement<?>) result;
                    }
                }
            }

            throw new RuntimeException("No matching ObjectFactory#create() method found for class: " + request.getClass());

        } catch (Exception e) {
            throw new RuntimeException("Failed to wrap object in JAXBElement via ObjectFactory", e);
        }
    }
}


if (requestPayload.getClass().isAnnotationPresent(XmlRootElement.class)) {
response = webServiceTemplate.marshalSendAndReceive(uri, requestPayload);
} else {
JAXBElement<?> wrapped = JaxbWrapperUtil.wrap(requestPayload);
response = webServiceTemplate.marshalSendAndReceive(uri, wrapped);
}

