package com.github.fnmartinez.jmicroconfigs;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ConfigurationManagerTest {

	static Stream<String> validEnvarPrefixFactory() {
		return Stream.of(
				"dummy",
				"DUMMY",
				"duMMy",
				"1dummy",
				"duh-my");
	}

	static Stream<String> validEnvironmentValuesFactory() {
		return Stream.of(
				null,
				"an_environment",
				"1",
				"other environment",
				"environment",
				"default");
	}

	static Stream<String> invalidEnvarPrefixFactory() {
		return Stream.of(
				"",
				" ",
				"\t",
				"\n",
				"\r",
				null,
				"\0",
				"=",
				"not a prefix");
	}

	static Stream<String> invalidEnvironmentValuesFactory() {
		return Stream.of(
				"",
				" ",
				"\t",
				"\n",
				"\r");
	}

	static Stream<Arguments> validConstructorParameters() {
		return Seq.seq(validEnvarPrefixFactory())
				.crossJoin(validEnvironmentValuesFactory())
				.map((arguments) -> Arguments.of(arguments.v1(), arguments.v2()))
				.stream();
	}

	@SuppressWarnings("unchecked")
	static Stream<Arguments> invalidConstructorParameters() {
		Seq envarPrefixValues = Seq.seq(validEnvarPrefixFactory())
				.concat(invalidEnvarPrefixFactory());
		Seq environmentValues = Seq.seq(validEnvironmentValuesFactory())
				.concat(invalidEnvironmentValuesFactory());
		return Seq.concat(
				Seq.seq(invalidEnvarPrefixFactory())
						.crossJoin(environmentValues)
						.map((arguments) -> Arguments.of(((Tuple2<String, String>)arguments).v1(),
								((Tuple2<String, String>)arguments).v2())),
				envarPrefixValues
						.crossJoin(invalidEnvironmentValuesFactory())
						.map((arguments) -> Arguments.of(((Tuple2<String, String>)arguments).v1(),
								((Tuple2<String, String>)arguments).v2())))
				.distinct()
				.ofType(Arguments.class)
				.stream();
	}

	@ParameterizedTest
	@MethodSource("validEnvarPrefixFactory")
	public void testValidArgumentsInConstructorWithOneParameter(String envarPrefix) {
		new ConfigurationManager(envarPrefix);
	}

	@ParameterizedTest
	@MethodSource("invalidEnvarPrefixFactory")
	public void testExceptionOnInvalidArgumentForConstructorWithOneParameter(String envarPrefix) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new ConfigurationManager(envarPrefix));
	}

	@ParameterizedTest
	@MethodSource("validConstructorParameters")
	public void testValidArgumentsInConstructorWithTwoParameters(String envarPrefix, String environment) {
		new ConfigurationManager(envarPrefix, environment);
	}

	@ParameterizedTest
	@MethodSource("invalidConstructorParameters")
	public void testExceptionOnInvalidArgumentForConstructorWithTwoParameters(String envarPrefix, String environment) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> new ConfigurationManager(envarPrefix, environment));
	}
}
