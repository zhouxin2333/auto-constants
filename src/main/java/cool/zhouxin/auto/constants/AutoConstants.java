package cool.zhouxin.auto.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhouxin
 * @since 2019/12/10 15:05
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AutoConstants {

    // 添加和导入一些其他类，一些不方便在类上上加@AutoContants的类
    // 比如第三方jar包中的类
    Class[] extra() default {};
}
