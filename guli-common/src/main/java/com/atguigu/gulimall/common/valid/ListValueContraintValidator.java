package com.atguigu.gulimall.common.valid;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ListValueContraintValidator implements ConstraintValidator<ListValue, Integer> {

    Set<Integer> set = new HashSet<>();
    
    @Override
    public void initialize(ListValue constraintAnnotation) {
        for (int val : constraintAnnotation.vals()) {
            set.add(val);
        }
    }

    @Override
    public boolean isValid(Integer var1, ConstraintValidatorContext var2) {
        return set.contains(var1);
    }
}
