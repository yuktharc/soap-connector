package com.yourorg.soap.util;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JAXBElementUtils {

    private static final Map<Class<?>, Method> methodCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> JAXBElement<T> wrapWithJAXBElement(T requestObj) {
        if (requestObj == null) throw new IllegalArgumentException("Request object cannot be null");
        Class<T> clazz = (Class<T>) requestObj.getClass();
        Package pkg = clazz.getPackage();

        try {
            // Try cached method or find a matching ObjectFactory method
            Method creator = methodCache.computeIfAbsent(clazz, c -> findBestMethod(c).orElse(null));

            if (creator != null) {
                Object factoryInstance = creator.getDeclaringClass().getDeclaredConstructor().newInstance();
                return (JAXBElement<T>) creator.invoke(factoryInstance, requestObj);
            }

            // Fallback: Build QName manually using class name and package-level namespace
            QName fallbackQName = buildQNameFromClass(clazz);
            return new JAXBElement<>(fallbackQName, clazz, requestObj);

        } catch (Exception e) {
            throw new RuntimeException("Failed to wrap request object with JAXBElement", e);
        }
    }

    private static Optional<Method> findBestMethod(Class<?> clazz) {
        String factoryClassName = clazz.getPackage().getName() + ".ObjectFactory";
        try {
            Class<?> factoryClass = Class.forName(factoryClassName);

            for (Method method : factoryClass.getMethods()) {
                if (!method.getName().startsWith("create")) continue;
                if (method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].equals(clazz)) continue;

                if (JAXBElement.class.isAssignableFrom(method.getReturnType())) {
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType pt) {
                        Type argType = pt.getActualTypeArguments()[0];
                        if (argType instanceof Class<?> type && type.equals(clazz)) {
                            return Optional.of(method);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {}

        return Optional.empty();
    }

    private static QName buildQNameFromClass(Class<?> clazz) {
        String namespace = extractNamespace(clazz);
        String localPart = decapitalize(clazz.getSimpleName());
        return new QName(namespace, localPart);
    }

    private static String extractNamespace(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        XmlSchema schema = pkg.getAnnotation(XmlSchema.class);
        return schema != null ? schema.namespace() : "";
    }

    private static String decapitalize(String name) {
        return (name == null || name.length() == 0)
                ? name
                : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
