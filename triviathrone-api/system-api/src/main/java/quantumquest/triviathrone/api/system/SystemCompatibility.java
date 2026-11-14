package quantumquest.triviathrone.api.system;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the system API major compatibility level required by a system provider.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SystemCompatibility {
    /**
     * Returns the required system API major version.
     */
    int major();
}
