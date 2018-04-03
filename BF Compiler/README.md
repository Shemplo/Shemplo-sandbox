# Brainfuck compiler

### About compiler

This compiler takes source code on *BF* and translate it into a *Java Byte Code*

* Build maven project:

```
mvn package
```

* Run a compiler:

```
java -cp dependency-jars/*;bf.compiler-0.1.0.jar ru.shemplo.bf.compiler.Run [src]
```

As `src` here can be path to the file (if file exists than it will be compiled) or raw *BF* code.

The result will be placed near `bf.compiler-*.jar` in class `Brainfuck.class`.

* Run a compiled code:

```
java Brainfuck
```
#### Specification

The tape after compilation is almost **ENDLESS**: the exact length of tape is
`Integer.MAX_VALUE` but it is looped. That means that after reaching of
the end of the tape, carriage will be moved to another end 
(Due to this application will never fall down with an <b>IndexOutOfBoundsException</b>,
<ins>but data in cell can be override</ins>).

### About brainfuck language

> **Turing-complete** programming language with 8 instructions

The execution of program can be compared with operations on *Turing machine*:
there is a endless (almost) tape with cells where can be stored some values
and also a carriage that points just one cell.

An example of possible tape condition:

```
TAPE:     ...[7][5][2][-2][4][6]... |  OR  | ...[0][0][0][0]...
CARRIAGE:        ^                  | JUST |        ^
OFFSET:   ...-1  0  1  2   3  4 ... | >>>> | ...-1  0  1  2 ...
```

#### Instructions
* `>` - move carriage by offset `+1` (one right)
* `<` - move carriage by offset `-1` (one left)
* `+` - change cell value by `+1` (increment)
* `-` - change cell value by `-1` (decrement)
* `.` - print cell value
* `,` - read cell value
* `[` - start new cycle
* `]` - close cycle body

#### Extra
* `:` - print cell value as a character
* `#` - one-line comment (till the end of line `\n`)

An example *how to print a number 72*:

```
++++++++++[>+++++++<-]>++.
\________/\__________/\__/
 1         2           3
 
(1) put on 0 offset value 10
...[0][10][0]...
       ^
...-1  0   1 ...

(2) makes a cycle like 'while (tape [0] != 0) { tape [1]++; }'
...[0][10][0]...
           ^
...-1  0   1 ...
----------------
...[0][10][7]...
           ^
...-1  0   1 ...
----------------
...[0][9][7]...
       ^
...-1  0  1 ...
----------------
9 more times ...
----------------
...[0][0][70]...
       ^
...-1  0  1 ...

(3) goes to 1 offset, add there 2 and print
...[0][0][72]...
          ^
...-1  0  1 ...
```

See more about BF on [wiki](https://en.wikipedia.org/wiki/Brainfuck)

### Verification

For check that it works you can put such code in `src.bf` file and then compile it

```brainfuck
# This is a one-line comment
# Compute Fibonacci number
>++++++++++>+>+[
    [+++++[>++++++++<-]>:<++++++[>--------<-]+<<<]>:>>[
        [-]<[>+<-]>>[<<+>+>-]<[>+<-[>+<-[>+<-[>+<-[>+<-[>+<-
            [>+<-[>+<-[>+<-[>[-]>+>+<<<-[>+<-]]]]]]]]]]]+>>>
    ]<<<
]
```

This code computes Fibonacci number

Where `:` sign means print cell value as a character (custom instruction)