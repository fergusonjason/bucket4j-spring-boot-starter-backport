package com.giffing.bucket4j.spring.boot.starter.config.filter.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.server.reactive.ServerHttpRequest;

import com.giffing.bucket4j.spring.boot.starter.config.filter.servlet.predicate.ServletMethodPredicate;
import com.giffing.bucket4j.spring.boot.starter.config.filter.servlet.predicate.ServletPathExecutePredicate;
import com.giffing.bucket4j.spring.boot.starter.context.ExecutePredicate;
import com.giffing.bucket4j.spring.boot.starter.context.ExecutePredicateDefinition;
import com.giffing.bucket4j.spring.boot.starter.context.FilterMethod;
import com.giffing.bucket4j.spring.boot.starter.context.constraintvalidations.Bucket4JConfigurationPredicateNameValidator;
import com.giffing.bucket4j.spring.boot.starter.context.properties.Bucket4JConfiguration;
import com.giffing.bucket4j.spring.boot.starter.context.properties.RateLimit;

class ConfigPredicateNameValidatorTest {

    List<ExecutePredicate<?>> executePredicates;
    Bucket4JConfigurationPredicateNameValidator validator;

    @BeforeEach
    void setup() {
		executePredicates = Arrays.asList(
				//Servlet predicates
				new ServletPathExecutePredicate(), new ServletMethodPredicate()
		);
        validator = new Bucket4JConfigurationPredicateNameValidator(executePredicates);
    }

    /**
     * Validate that all filtermethods pass the test if they don't have predicates
     */
    @ParameterizedTest
    @EnumSource(FilterMethod.class)
    void testValidConfigurationWithoutPredicates(FilterMethod filterMethod) {
        Bucket4JConfiguration configuration = setupConfiguration(filterMethod, List.of(), List.of());
        testValidPredicates(configuration);
    }

    /**
     * Validate that SERVLET and WEBFLUX pass the test with a valid PATH execute-predicate
     */
    @ParameterizedTest
    @EnumSource(value = FilterMethod.class, names = {"SERVLET"})
    void testValidServletExecutePredicate(FilterMethod filterMethod) {
        List<String> executePredicates = List.of("PATH=valid-predicate");
        List<String> skipPredicates = List.of();
        Bucket4JConfiguration configuration = setupConfiguration(filterMethod, executePredicates, skipPredicates);

        testValidPredicates(configuration);
    }

    /**
     * Validate that SERVLET and WEBFLUX pass the test with a valid PATH skip-predicate
     */
    @ParameterizedTest
    @EnumSource(value = FilterMethod.class, names = {"SERVLET"})
    void testValidServletSkipPredicate(FilterMethod filterMethod) {
        List<String> executePredicates = List.of();
        List<String> skipPredicates = List.of("PATH=valid-predicate");
        Bucket4JConfiguration configuration = setupConfiguration(filterMethod, executePredicates, skipPredicates);

        testValidPredicates(configuration);
    }


    /**
     * Validate that custom predicates are supported
     */
    @Test
    void customPredicateTest() {
        List<ExecutePredicate<?>> includingCustomPredicate = new ArrayList<>(this.executePredicates);
		includingCustomPredicate.add(new CustomTestPredicate());

        Bucket4JConfigurationPredicateNameValidator customPredicateValidator =
                new Bucket4JConfigurationPredicateNameValidator(includingCustomPredicate);

        List<String> executePredicates = List.of("CUSTOM-QUERY=custom-servlet");
        List<String> skipPredicates = List.of();
        Bucket4JConfiguration configuration = setupConfiguration(FilterMethod.SERVLET, executePredicates, skipPredicates);

        ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);
        assertTrue(customPredicateValidator.isValid(configuration, context));
    }

    private Bucket4JConfiguration setupConfiguration(
            FilterMethod filterMethod,
            List<String> executePredicates,
            List<String> skipPredicates
    ) {
        Bucket4JConfiguration configuration = new Bucket4JConfiguration();
        configuration.setFilterMethod(filterMethod);

        RateLimit rateLimit = new RateLimit();
        rateLimit.setExecutePredicates(executePredicates.stream().map(ExecutePredicateDefinition::new).collect(Collectors.toList()));
        rateLimit.setSkipPredicates(skipPredicates.stream().map(ExecutePredicateDefinition::new).collect(Collectors.toList()));
        configuration.setRateLimits(Collections.singletonList(rateLimit));

        return configuration;
    }

    private String getInvalidPredicateMessage(List<String> expectedInvalidNames){
        return "Invalid predicate name" + (expectedInvalidNames.size() > 1 ? "s" : "") + ": " +
                String.join(", ", expectedInvalidNames);
    }

    private void testValidPredicates(Bucket4JConfiguration configuration) {
        ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);
        assertTrue(this.validator.isValid(configuration, context));
    }

    private void testInvalidPredicates(Bucket4JConfiguration configuration, String expectedError) {
        ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);
        assertFalse(this.validator.isValid(configuration, context));

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(context).buildConstraintViolationWithTemplate(contextCaptor.capture());

        assertEquals(expectedError, contextCaptor.getValue());
    }


    private class CustomTestPredicate extends ExecutePredicate<HttpServletRequest> {

        @Override
        public String name() {
            return "CUSTOM-QUERY";
        }

        @Override
        protected ExecutePredicate<HttpServletRequest> parseSimpleConfig(String simpleConfig) {
            return this;
        }

        @Override
        public boolean test(HttpServletRequest httpServletRequest) {
            return false;
        }
    }
}
