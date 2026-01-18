package io.github.dhruv1503.bootusage.autoconfigure;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that captures bean class to code source location mappings.
 * <p>
 * This provides runtime bean origin tracking by inspecting the {@link ProtectionDomain}
 * of each bean's class to determine the JAR or directory from which it was loaded.
 * <p>
 * Infrastructure beans (Spring framework internals) are skipped to reduce noise
 * and improve performance.
 *
 * @author Dhruv
 * @since 0.1.0
 */
public class BeanOriginTrackingPostProcessor implements BeanPostProcessor {

    private final Map<String, String> beanOrigins = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = bean.getClass();
        // Skip core Spring infrastructure for clarity and performance
        String typeName = type.getName();
        if (typeName.startsWith("org.springframework.") ||
            typeName.startsWith("java.") ||
            typeName.startsWith("javax.") ||
            typeName.startsWith("jakarta.") ||
            typeName.contains("$$")) { // Skip proxies
            return bean;
        }
        String location = findLocation(type);
        if (location != null) {
            this.beanOrigins.put(beanName, location);
        }
        return bean;
    }

    private String findLocation(Class<?> type) {
        try {
            ProtectionDomain pd = type.getProtectionDomain();
            if (pd == null) {
                return null;
            }
            CodeSource cs = pd.getCodeSource();
            if (cs == null) {
                return null;
            }
            URL url = cs.getLocation();
            return (url != null ? url.toString() : null);
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * Returns an unmodifiable view of the collected bean origins.
     *
     * @return map of bean name to code source location
     */
    public Map<String, String> getBeanOrigins() {
        return Collections.unmodifiableMap(this.beanOrigins);
    }
}
