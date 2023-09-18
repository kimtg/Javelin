#!/bin/sh
mkdir bin 2> /dev/null
javac -d bin src/javelin/*.java
jar cvfe javelin.jar javelin.Core -C bin .
