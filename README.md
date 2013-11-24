# YEBISU

**YEBISU** (**YE**t another **B**ytecode **I**nstrumentation agent for **S**oftware **U**nderstanding) is a toolkit for conducting dynamic analysis on Java applications.
It observes runtime behavior of a subject application and records execution events to a trace log.

## How it works

### On-the-fly bytecode instrumentation

The agent is implemented based on `java.lang.instrument` API.
It hooks to Java Virtual Machine (JVM) and intercept each Java class being loaded.

When a class being loaded is of interest, event notification calls are instrumented by using bytecode transformation technique.
This transformation is done on the fly; **classes (*.class) and JAR files on the file system are not modified** by the agent.

### Execution Events

When the instrumented classes are executed, the following events are notified back to the agent and recoded in trace log files.

* `ClassLoad (classId, className)`:
	Notifies a class being loaded to the virtual machine. A unique identifier and a fully qualified name of the class are given as parameters.

* `MethodInstrument (classId, methodId, methodName)`:
	Notifies a method of a class is being instrumented. Unique identifiers of the class and the methods and a fully qualified name of the method are given as parameters.

* `MethodEntry (methodId, context, arguments)`:
	Notifies execution of an instrumented method has begun.
	The context of the method (i.e. `this` object) and the arguments of the invocation are given as parameters.

* `MethodExit (methodId, returnValue)`
	Notifies execution of an instrumented method has just finished.
	The context of the method (i.e. `this` object) and the return value are given as parameters.

* `JVMShutdown`:
	Notifies the virtual machine is shutting down. After recording this event, trace log file is closed.
	No parameter is given with this event.

## System Requirements

* Java Runtime Environment (JRE) 1.5 or higher

## Install

You will find the follwing two files in the distribution.
Their location does not matter: place them anywhere you like.

* `yebisu.jar` : JAR library containing the agent
* `yebisu properties` : configuration file for the agent

## Usage

### Configuring the agent

Open the `yebisu.properties` file in your text editor and modify configurations for the agent.
In many cases you may want to change the following items.

* Location of trace log files

	Specify a location and a file name pattern for the trace log file.
	A relative path is traversed from the working (current) directory of the `java` command.
	By default, startup time of the agent is shown as a part of the file name.

		yebisu.event.csv.file.pattern=./yebisu-event-{0,date,yyyyMMddHHmmss}.csv

* Prefixes of class names to be observed

	Observing all classes in an application may cause too much overhead and make applications unacceptably slow.
	To limit the number of classes/methods to be observed, you had better specify package names of your interest.

		yebisu.include.prefix.0=my.application.package.

* Prefixes of class names **NOT** to be observed

	Specify package names that are not of your interest.
	By default, the agent ignores classes in YEBISU (`yebusu.jar`), JRE runtime library and other 3rd party libraries.
	Observing them may cause JVM crash or produce too much logs.
	Some commercial libraries are not allowed to be modified by their terms and conditions.

		yebisu.exclude.prefix.0=jp.ac.titech.cs.se.yebisu.
		yebisu.exclude.prefix.1=java.
		yebisu.exclude.prefix.2=javax.
		yebisu.exclude.prefix.3=sun.
		yebisu.exclude.prefix.4=com.sun.
		yebisu.exclude.prefix.5=apple.
		yebisu.exclude.prefix.6=com.apple.


### Observing a standalone application

To analyze a Java application, run `java` command with additional `-javaagent` option to give the location of `yebisu.jar`.

	java -javaagent:/path/to/yebisu.jar Main

Here, `Main` is a main class of your application.
By default, yebisu.properties file is read from working (current) directory of the `java` command. 
Instead, you can tell the agent to use a property file in the other location:

	java -javaagent:/path/to/yebisu.jar=/foo/bar/yebisu.properties Main

### Observing a web application

The agent can observe Java EE web applications, by adding `-javaagent` option to startup scripts of application servers.
The location of startup scripts to be modified depends on your server products.

#### Apache Tomcat

For example, [Apache Tomcat](http://tomcat.apache.org/) sources `${CATALINA_HOME}/bin/setenv.sh` (or `setenv.bat` in Windows) in its startup script.
You can specify the `-javaagent` option in the setenv script.

* UNIX, Linux and Mac: ${CATALINA_HOME}/bin/setenv.sh

		export JAVA_OPTS="-javaagent:/path/to/yebisu.jar=/foo/bar/yebisu.properties"

* Windows: ${CATALINA_HOME}/bin/setenv.bat

		@echo off
		set JAVA_OPTS="-javaagent:C:\path\to\yebisu.jar=C:\foo\bar\yebisu.properties"

### Obtaining execution traces

Observed events are recorded in the trace log file specified in the `yebisu.properties` file.
Trace logs conforms CSV (comma separeted values) format and currently we do not bundle any analysis tool for them.
Write your own log parsers to fit your needs.

## Compile and build

YEBISU is built by using [Apache Maven](http://maven.apache.org/).
Before you proceed, download Maven and configure the environment variable `M2_HOME` properly.

1. Fork (or download) the source code.
2. Open your terminal or command prompt, move to the directory containing pom.xml and hit `mvn package` there.
3. After maven build succeeds, you will find `yebisu.jar` and `yebisu.properties` in `target/` subdirectory.

## References

YEBISU is designed for stimulating researchers in software engineering.
Feel free to use it in your research and publications.
Please cite the following article as its source:

* Hiroshi Kazato, Shinpei Hayashi, Tsuyoshi Oshima, Shunsuke Miyata, Takashi Hoshino, Motoshi Saeki: "Extracting and Visualizing Implementation Structure of Features". In Proceedings of the 20th Asia-Pacific Software Engineering Conference (APSEC 2013). Bangkok, Thailand, dec, 2013.

## Copyright and license

Copyright (c) 2010-2012 [Saeki Lab.](http://www.se.cs.titech.ac.jp/) at [Tokyo Institute of Technology](http://www.titech.ac.jp/).  
YEBISU is an open source software, licensed under the [Apache Licese, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Acknowledgment

This software includes [Javassisst](http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/) developed by [Shigeru Chiba](http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/site/).