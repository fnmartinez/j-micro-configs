# j-micro-configs
[![Build Status](https://www.travis-ci.org/fnmartinez/j-micro-configs.svg?branch=master)](https://www.travis-ci.org/fnmartinez/j-micro-configs) [![codecov](https://codecov.io/gh/fnmartinez/j-micro-configs/branch/master/graph/badge.svg)](https://codecov.io/gh/fnmartinez/j-micro-configs)

Flexible and easy configurations for simple Java micro-services.


## What is this?

`j-micro-configs` is a simple library with a small memory footprint, that enables to easily configure Java 
micro-services.  

It is inspired in the way Spring-Boot manages [configurations](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).

The library provides the following features out of the box:

  * Support for configuration files (only YAML files for the moment)
  * Support for different environments in the same configuration file
  * Support for a default environment, with default values
  * Support to override configuration values with Environment Variables
  
  
## Why should I use this?

Why would I use this instead of some other awesome and well tested configuration libs like Apache Config?

Well, there is no reason why you shouldn't go to other libs. But in my experience, although they are great libs with 
many features that this lib doesn't have, they are quite complex to use. ~~j-micro-configs boasts in its ease of use. And
in its small memory footprint.~~ j-micro-configs boasts in two things: its ease of use and small memory footprint. 
And out of the box support to override configuration values with Environment Varia-- ok, this is moot. The thing is, 
it's simple and it has many features out of the box that makes it cool.

## How do I use this?

Simply add it to your POM (or build.gradle, or whatever you use to build your Java micro-service) as a dependency. Since 
it is uploaded into the Maven Central Repository, you can simply add the following:

##### Maven:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>com.github.fnmartinez</groupId>
        <artifactId>j-micro-configs</artifactId>
        <version>1.0.0</version>
    </dependency>
    ...
</dependencies>
```
##### Gradle:
```groovy
dependencies {
    ...
    compile group: 'com.github.fnmartinez', name: 'j-micro-configs', version: '1.0.0'
    ...
}
```
Then create a file named `applications.yml`, and add it to your resources.

Afterwards you only need to instantiate the ConfigurationManager and load you configs like this:

```java
import com.github.fnmartinez.jmicroconfigs.ConfigurationManager
...
    ConfigurationManager cm = new ConfigurationManager("MY_APP");
    cm.loadConfigs();
...
```

And it will be all setup! You only need to get your variables from the ConfigurationManager instance like this:

```java
int anInt = cm.getInt("path.to.my.int");
String s = cm.getString("path.to.my.string")
```

There are many types of values you can get out from your configs. The currently supported types are:

  * Byte
  * Short
  * Integer
  * Long
  * Boolean
  * Char
  * Float
  * Double
  * Object
  
The type Object is for when your config uses something complex defined by the YAML standard, such as a list or a map. 
You will have to cast it, though, to make sure it behaves as expected. Or is whatever you were looking for.

As you can see, to get a variable from your configs, you have to fetch it with a path. That path is the one defined in 
your YAML configuration file. So if you have a YAML like this one

```yaml
environment: default
my:
  awesome:
    app:
      configs:
        anInt: 1
        aString: hi!
        aBoolean: true
        myListOfChar:
          - a
          - b
          - c
          - d
        myMapOfValues:
          a: 1
          b: 2
          c: 3
```

To get the int you should use the method:

```java
cm.getInt("my.awesome.app.configs.anInt");
```

To get the boolean you should use the method:

```java
cm.getBoolean("my.awesome.app.configs.aBoolean");
```

And to get the list:

```java
List l = (List) cm.getObject("my.awesome.app.configs.myListOfChar");
```

If you wish, you can also fetch a value from your list directly, by indicating its index:

```java
char c = cm.getChar("my.awesome.app.configs.myListOfChar[0]");
```

Though I think is better the former ;)


### Working with environments

As you can see in the YAML provided before, there was an element named `environment` which value was `default`. Now, 
although this element is not mandatory it certainly is a good starter for this section. That element is one of the only 
two reserved keywords that this library forces to the user. The other being `default` for the value of this `environment`
keyword.

So, what is an *environment*, anyway? Well, an environment is a set of values for your configuration properties that 
must be held together. Like when you are in CI environment and you want your Database string to be fixed, but when you 
are in a staging or productive environment you want it to be custom. Or when you want to configure your logs for something
different in every environment. That is what j-micro-configs' *environments* come to solve.

As said, it is not mandatory to have the `environment` element in your config file. But you must take into account that 
if no `environment` element is present in the YAML file, then the `default` value will be assigned. And since you cannot 
have two environments with the same name, a nasty exception will be risen.

#### How to use different environments?

Let's imagine the following case: We have an app called MyApp, where we will have a DB to which it will connect to. And 
so we have various different sets of values for each environment. One for the development environment, one for the 
testing/CI environment, and the last one its for the deploying case. We would also like to have some common values 
between them. So our `application.yml` file would look like this:

```yaml
db:
  user: my_awesome_app_user
---
environment: development
db:
  password: simple_pass
  host: localhost
---
environment: ci-testing
db:
  password: testing
  host: db-host
---
environment: deployment
db:
  user:
  password:
  host:
```

So, as you can see, we setup our `default` environment by not indicating an environment element. In this environment we 
only indicate our DB username.  
For the development and and testing environment, we indicate our password and the host. But we leave the username out. 
This will make j-micro-configs to look for the value of the user in the default environment.  
Now, for the deployment environment we hint that all values should be taken from deployment environment and not from file.
Since pushing deployment configuration is bad for your health.

A clarification on the last one. j-micro-configs won't know per se that the values should be looked only in the 
environment variables, but I'm guessing that you app won't handle well a connection to an empty string. So it should 
signal you the variables aren't there.


### Using environment variables

As we talked before, j-micro-configs will take precedence on the configurations found in the environment variables over 
the ones specified in the YAML files. So continuing with the example of the previous section, in our deployment environment
we will have to use environment variables to make sure that our configs reach our app. In order to do so, we will name them
the following way:  
  * `MY_APP__DB__USER`
  * `MY_APP__DB__PASSWORD`
  * `MY_APP__DB__HOST`
  
The double underscore is the thing we will use to separate our paths in the variables definitions. This will help j-micro-configs
discern between a variable name and an sub-path. 

The `MY_APP` prefix is what j-micro-configs will look after in the environment variables to know that they belong to our 
app exclusively. This prefix is the one we pass in the constructor:

```java
ConfigurationManager cm = new ConfigurationManager("MY_APP");
```

At the moment there is no way to get variables that don't have the app name prefix. This might be a feature for future 
revisions or versions.
