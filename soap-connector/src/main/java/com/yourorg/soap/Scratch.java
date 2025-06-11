package com.yourorg.soap.util;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JaxbElementWrapperUtil {

    public static JAXBElement<?> toJaxbElement(Object payload, String elementNameHint) {
        if (payload == null) throw new IllegalArgumentException("Payload cannot be null");

        Class<?> payloadClass = payload.getClass();
        String factoryClassName = payloadClass.getPackageName() + ".ObjectFactory";

        try {
            Class<?> factoryClass = Class.forName(factoryClassName);
            Object factoryInstance = factoryClass.getDeclaredConstructor().newInstance();

            List<Method> matchingMethods = new ArrayList<>();

            for (Method method : factoryClass.getMethods()) {
                if (!JAXBElement.class.isAssignableFrom(method.getReturnType())) continue;
                if (method.getParameterCount() != 1) continue;

                Class<?> paramType = method.getParameterTypes()[0];
                if (!paramType.isAssignableFrom(payloadClass)) continue;

                XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
                if (decl == null) continue;

                // Match element name if hint is provided
                if (elementNameHint == null || decl.name().equalsIgnoreCase(elementNameHint)) {
                    matchingMethods.add(method);
                }
            }

            if (matchingMethods.isEmpty()) {
                throw new RuntimeException("No ObjectFactory method found for class: " + payloadClass.getName() +
                        (elementNameHint != null ? " with element name: " + elementNameHint : ""));
            }

            if (matchingMethods.size() == 1) {
                return (JAXBElement<?>) matchingMethods.get(0).invoke(factoryInstance, payload);
            }

            // Try to resolve using class name suffix heuristic if still ambiguous
            String strippedName = stripSuffix(payloadClass.getSimpleName(), "Type");
            for (Method method : matchingMethods) {
                XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
                if (decl.name().equalsIgnoreCase(strippedName)) {
                    return (JAXBElement<?>) method.invoke(factoryInstance, payload);
                }
            }

            // If still ambiguous
            StringBuilder msg = new StringBuilder("Multiple matching factory methods found for ")
                    .append(payloadClass.getSimpleName()).append(" with element name '")
                    .append(elementNameHint).append("'.\nAvailable matches:\n");

            for (Method method : matchingMethods) {
                XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
                msg.append("- QName: {").append(decl.namespace()).append("}").append(decl.name())
                        .append(" in method: ").append(method.getName()).append("\n");
            }

            throw new IllegalStateException(msg.toString());

        } catch (Exception ex) {
            throw new RuntimeException("Failed to wrap object in JAXBElement: " + ex.getMessage(), ex);
        }
    }

    private static String stripSuffix(String input, String suffix) {
        return input.endsWith(suffix) ? input.substring(0, input.length() - suffix.length()) : input;
    }
}
