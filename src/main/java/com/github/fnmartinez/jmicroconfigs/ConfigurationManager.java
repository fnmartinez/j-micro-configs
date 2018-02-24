package com.github.fnmartinez.jmicroconfigs;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationManager {
	public static final String ENVIRONMENT_VARIABLE_PATH_HIERARCHY_DIFFERENTIATOR = "__";

	public static final String DEFAULT_ENVIRONMENT_NAME = "default";
	public static final String ENVIRONMENT_KEY_WORD = "environment";

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final Map<String, Map<String, Object>> environments = Maps.newHashMap();

	private final Map<String, String> environmentVariables = Maps.newHashMap();

	private final String envarPrefix;

	private String environment;

	public ConfigurationManager(String envarPrefix) {
		this(envarPrefix, null);
	}

	public ConfigurationManager(String envarPrefix, String environment) {
		logger.entry(envarPrefix, environment);
		try {
			assertThat(envarPrefix)
					.isNotNull()
					.isNotEmpty()
					.isNotEqualToIgnoringWhitespace(Strings.EMPTY)
					.doesNotMatch("^.* .*$")
					.doesNotContain("=")
					.doesNotContain("\0");
			if (environment != null) {
				assertThat(environment)
						.isNotEmpty()
						.isNotEqualToIgnoringWhitespace(Strings.EMPTY);
			}
		} catch (AssertionError ae) {
			logger.catching(ae);
			throw new IllegalArgumentException(ae);
		}
		this.envarPrefix = envarPrefix.trim();
		this.environment = environment;
		logger.exit(this);
	}

	private String fetchEnvironment() {
		String environment = getEnVar(ENVIRONMENT_KEY_WORD);
		if (environment == null || environment.trim().equals("")) {
			return DEFAULT_ENVIRONMENT_NAME;
		}
		return environment;
	}

	private String getEnVar(String path) {
		String envarpath = toEnVarPath(path);
		return System.getenv(envarpath);
	}

	private String toEnVarPath(String path) {
		String[] levels = path.split("\\.");
		for (int i = 0; i < levels.length; i++) {
			levels[i] = levels[i].replace("[", "--").replace("]", "--");
			levels[i] = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, levels[i]);
		}
		StringBuilder sb = new StringBuilder(envarPrefix);
		for (String level: levels) {
			sb.append(ENVIRONMENT_VARIABLE_PATH_HIERARCHY_DIFFERENTIATOR);
			sb.append(level);
		}
		return sb.toString();
	}

	public void loadConfigs() throws FileNotFoundException {
		ClassLoader classLoader = this.getClass().getClassLoader();
		loadConfigs(classLoader.getResourceAsStream("application.yml"));
	}

	public void loadConfigs(String configFilePath) throws IOException {
		loadConfigs(Paths.get(configFilePath));
	}

	public void loadConfigs(Path configFilePath) throws IOException {
		loadConfigs(Files.newInputStream(configFilePath, StandardOpenOption.READ));
	}

	public void loadConfigs(InputStream configFileIs) throws FileNotFoundException {
		loadFileConfigs(configFileIs);
		overrideWithEnvironment();
		if (!environments.containsKey(environment)) {
			throw new IllegalStateException(format("Provided environment %s does not exists.", environment));
		}
	}

	@SuppressWarnings("unchecked")
	private void loadFileConfigs(InputStream configFileIs) throws FileNotFoundException {
		Yaml yaml = new Yaml();
		Iterable<Object> configs = yaml.loadAll(configFileIs);
		for (Object config: configs) {
			if (!(config instanceof Map)) {
				throw new IllegalArgumentException("Unsupported configuration type. " +
						"All configuration files should be maps/objects representations.");
			}
			createEnvironmentConfig((Map<String, Object>)config);
		}
	}

	private void overrideWithEnvironment() {
		if (environment == null) {
			environment = fetchEnvironment();
		}
		Map<String, String> environmentVariables = System.getenv().entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(this.envarPrefix + ENVIRONMENT_VARIABLE_PATH_HIERARCHY_DIFFERENTIATOR))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		this.environmentVariables.clear();
		this.environmentVariables.putAll(environmentVariables);
	}

	private void createEnvironmentConfig(Map<String, Object> configMap) {
		String environment;
		if (configMap.containsKey(ENVIRONMENT_KEY_WORD)) {
			environment = (String)configMap.get(ENVIRONMENT_KEY_WORD);
		} else {
			environment = DEFAULT_ENVIRONMENT_NAME;
		}
		if (this.environments.containsKey(environment)) {
			throw new IllegalArgumentException(format("Duplicated environment: %s", environment));
		} else {
			Map<String, Object> envConfig = Maps.newHashMap();
			configMap.entrySet().stream()
					.filter(entry -> entry.getKey().compareTo(ENVIRONMENT_KEY_WORD) != 0)
					.forEach(entry -> envConfig.put(entry.getKey(), entry.getValue()));
			this.environments.put(environment, envConfig);
		}
	}

	private Object fetchInEnvironmentVariables(String path) {
		return environmentVariables.get(toEnVarPath(path));
	}

	@SuppressWarnings("unchecked")
	private Object fetchInConfigFile(String path) {
		String[] splitPath = path.split("\\.");
		Object config = this.environments.get(environment);
		for (String level: splitPath) {
			if (level.matches("^.*\\[\\d+\\]$")) {
				int indexStart = level.lastIndexOf('[');
				int indexEnd = level.length() - 1;
				String configName = level.substring(0, indexStart);
				int index = Integer.valueOf(level.substring(indexStart + 1, indexEnd));
				List cs = ((Map<String, List<?>>)config).get(configName);
				if (cs == null) {
					return null;
				}
				config = cs.get(index);
			} else {
				config = ((Map<String, Object>)config).get(level);
			}
		}
		return config;
	}

	public Object get(String path) {
		Object config = fetchInEnvironmentVariables(path);
		if (config == null) {
			config = fetchInConfigFile(path);
		}
		return config;
	}

	public byte getByte(String path) {
		return Byte.valueOf(get(path).toString());
	}

	public short getShort(String path) {
		return Short.valueOf(get(path).toString());
	}

	public int getInt(String path) {
		return Integer.valueOf(get(path).toString());
	}

	public long getLong(String path) {
		return Long.valueOf(get(path).toString());
	}

	public boolean getBoolean(String path) {
		return Boolean.valueOf(get(path).toString());
	}

	public char getChar(String path) {
		return get(path).toString().charAt(0);
	}

	public String getString(String path) {
		return get(path).toString();
	}

	public float getFloat(String path) {
		return Float.valueOf(get(path).toString());
	}

	public double getDouble(String path) {
		return Double.valueOf(get(path).toString());
	}
}
