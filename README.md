# j-micro-configs
[![Build Status](https://www.travis-ci.org/fnmartinez/j-micro-configs.svg?branch=master)](https://www.travis-ci.org/fnmartinez/j-micro-configs) [![codecov](https://codecov.io/gh/fnmartinez/j-micro-configs/branch/master/graph/badge.svg)](https://codecov.io/gh/fnmartinez/j-micro-configs)

Flexible and easy configurations for simple Java micro-services.


## What is this?

`j-micro-configs` is a simple library with a small memory footprint, that enables to flexibly configure Java 
micro-services.  

It is inspired in the way Spring-Boot manages [configurations](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).

The library provides the following features out of the box:

  * Support for configuration files (only YAML files for the moment)
  * Support for different environments in the same configuration file
  * Support for a default environment, with default values
  * Support to override configuration values with Environment Variables
