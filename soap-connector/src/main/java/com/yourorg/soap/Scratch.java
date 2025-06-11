package com.yourorg.soap.util;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JaxbElementWrapperUtil {

    public static JAXBElement<?> toJaxbElement(Object payload) {
        if (payload == null) throw new IllegalArgumentException("Payload cannot be null");

        Class<?> payloadClass = payload.getClass();
        String factoryClassName = payloadClass.getPackageName() + ".ObjectFactory";

        try {
            Class<?> factoryClass = Class.forName(factoryClassName);
            Object factoryInstance = factoryClass.getDeclaredConstructor().newInstance();

            List<Method> candidateMethods = new ArrayList<>();

            for (Method method : factoryClass.getMethods()) {
                if (!JAXBElement.class.isAssignableFrom(method.getReturnType())) continue;
                if (method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(payloadClass)) continue;

                XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
                if (decl != null) {
                    candidateMethods.add(method);
                }
            }

            if (candidateMethods.isEmpty()) {
                throw new RuntimeException("No factory method found for class: " + payloadClass.getName());
            }

            if (candidateMethods.size() == 1) {
                return (JAXBElement<?>) candidateMethods.get(0).invoke(factoryInstance, payload);
            }

            // Attempt to resolve ambiguity based on best match with class name
            String simpleClassName = payloadClass.getSimpleName().replaceAll("Type$", "");
            for (Method method : candidateMethods) {
                XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
                if (decl != null && decl.name().equalsIgnoreCase(simpleClassName)) {
                    return (JAXBElement<?>) method.invoke(factoryInstance, payload);
                }
            }

            // Still ambiguous
            StringBuilder msg = new StringBuilder("Multiple ObjectFactory methods found for ")
                    .append(payloadClass.getName())
                    .append(". Please supply a QName or override mapping:\n");

            for (Method m : candidateMethods) {
                XmlElementDecl decl = m.getAnnotation(XmlElementDecl.class);
                msg.append("- QName: {").append(decl.namespace()).append("}").append(decl.name()).append("\n");
            }

            throw new IllegalStateException(msg.toString());

        } catch (Exception ex) {
            throw new RuntimeException("Failed to wrap object in JAXBElement: " + ex.getMessage(), ex);
        }
    }
}
