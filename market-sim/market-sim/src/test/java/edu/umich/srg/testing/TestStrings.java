package edu.umich.srg.testing;

import org.junit.experimental.theories.ParametersSuppliedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ParametersSuppliedBy(TestStringsSupplier.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestStrings {
  String[] value();
}
