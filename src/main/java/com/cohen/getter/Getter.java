package com.cohen.getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author linjincheng
 * @version v1.0.0
 * @date 2019/5/14 16:18
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Getter {
}
