package com.atguigu.gulimall.common.valid;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(
   validatedBy = {ListValueContraintValidator.class}
)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ListValue {
   String message() default "{com.atguigu.gulimall.common.valid.ListValue.message}";

   Class<?>[] groups() default {};

   Class<? extends Payload>[] payload() default {};

   int[] vals() default {};

}