package com.github.fnmartinez.jmicroconfigs;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ConfigurationManagerTest {
	static final String DEFAULT_ENVAR_PREFIX = "TEST";

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

	@Test
	public void testDefaultLoadConifgs() {
		ConfigurationManager configurationManager = new ConfigurationManager(DEFAULT_ENVAR_PREFIX);
		try {
			configurationManager.loadConfigs();
		} catch (FileNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	@Test
	public void testNonExistantConfigReturnsNull() {
		ConfigurationManager configurationManager = new ConfigurationManager(DEFAULT_ENVAR_PREFIX);
		try {
			configurationManager.loadConfigs();
		} catch (FileNotFoundException e) {
			throw new AssertionError(e);
		}
		Object o = configurationManager.get("my.nested.config.an_int");
		assertThat(o).isNotNull();
		o = configurationManager.get("does.not.exists");
		assertThat(o).isNull();
	}

	@Test
	public void testValueGetters() {
		ConfigurationManager configurationManager = new ConfigurationManager(DEFAULT_ENVAR_PREFIX);
		try {
			configurationManager.loadConfigs();
		} catch (FileNotFoundException e) {
			throw new AssertionError(e);
		}
		Integer aHexInt = configurationManager.getInt("my.nested.config.a_hex_int");
		Integer anOctalInt = configurationManager.getInt("my.nested.config.an_octal_int");
		Integer anInt = configurationManager.getInt("my.nested.config.an_int");
		Boolean aBoolean = configurationManager.getBoolean("my.nested.config.a_boolean");
		String string = configurationManager.getString("my.nested.config.a_string");
		Double aDouble = configurationManager.getDouble("my.nested.config.a_real");
		Date aDate = configurationManager.getDate("my.nested.config.a_date");
		Date aDateTime = configurationManager.getDate("my.nested.config.a_date_time");
		List aList = configurationManager.get("my.nested.config.a_list");
		assertThat(aHexInt).isEqualTo(0x01);
		assertThat(anOctalInt).isEqualTo(02);
		assertThat(anInt).isEqualTo(3);
		assertThat(aBoolean).isEqualTo(true);
		assertThat(string).isEqualTo("hello");
		assertThat(aDouble).isEqualTo(2.5);
		assertThat(aDate).isBetween("1988-12-03", "1988-12-05");
		assertThat(aDateTime).isBetween("1988-12-04T00:00:00", "1988-12-05T00:00:00");
		assertThat(aList).isNotNull().isNotEmpty();
	}

	@Test
	public void testListIndexGetter() {
		ConfigurationManager configurationManager = new ConfigurationManager(DEFAULT_ENVAR_PREFIX);
		try {
			configurationManager.loadConfigs();
		} catch (FileNotFoundException e) {
			throw new AssertionError(e);
		}
		List aList = configurationManager.get("my.nested.config.a_list");
		assertThat(aList).isNotNull().isNotEmpty();
		for (int i = 0; i < aList.size(); i++) {
			Object o = configurationManager.get(format("my.nested.config.a_list[%d]", i));
			assertThat(o).isEqualTo(aList.get(i));
		}
	}

	@Test
	public void testNonDefaultEnvironmentOverridesDefaultEnvironment() {
		ConfigurationManager configurationManager = new ConfigurationManager(DEFAULT_ENVAR_PREFIX, "non-default");
		try {
			configurationManager.loadConfigs();
		} catch (FileNotFoundException e) {
			throw new AssertionError(e);
		}
		Boolean aBoolean = configurationManager.get("does.not.exists");
		assertThat(aBoolean).isNotNull().isEqualTo(false);
		Integer anInt = configurationManager.get("my.nested.config.an_int");
		assertThat(anInt).isEqualTo(4);
		Integer otherInt = configurationManager.get("my.nested.config.other_int");
		assertThat(otherInt).isEqualTo(5);
		Double aDouble = configurationManager.get("my.nested.config.a_real");
		assertThat(aDouble).isNotNull().isEqualTo(2.5);
	}
}
