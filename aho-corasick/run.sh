#!/bin/bash
# Compile and run tests + benchmark (needs JDK 11+)
set -e
mkdir -p out
javac -d out src/*.java 2>/dev/null || java -m jdk.compiler/com.sun.tools.javac.Main -d out src/*.java
java -cp out Tests
java -cp out Benchmark
