@echo off
set "src=%~1"
java -cp dependency-jars/*;bf.compiler-0.1.0.jar ru.shemplo.bf.compiler.Run %src%
pause