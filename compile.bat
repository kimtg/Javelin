@echo off
mkdir bin 2> nul
javac -d bin src/javelin/*.java
jar cvfe javelin.jar javelin.Core -C bin .
