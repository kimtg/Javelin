# The Javelin Programming Language

Javelin is a dialect of Lisp. It is designed to be an embedded language (minimal Lisp for the Java Virtual Machine). It uses Clojure-like syntax.

## Influenced by
[Clojure](https://clojure.org/)

## Influenced
[Closhure](https://github.com/kimtg/Closhure)

## Key learnings
1. Lisp interpreter
2. Reflection
3. Multi-line input

## Compile ##
Run `compile.bat`. This script makes a runnable JAR file.

On Unix, use *.sh files.

## Run ##
Run `javelin.bat` or,
```
Usage:
java javelin.Core [OPTION] [ARGS...]
java -cp javelin.jar javelin.Core [OPTION] [ARGS...]
java -jar javelin.jar [OPTION] [ARGS...]

Options:
    FILE  run a script.
    -h    print this screen.
    -r    run a REPL.
    -v    print version.
Operation:
    Binds *command-line-args* to a list of strings containing command line args that appear after FILE.
```

Run `DrJavelin.bat` to run a simple GUI REPL.

## Reference ##
```
Special forms:
 . and catch def defmacro do doseq finally fn if import let loop new or quasiquote quote recur reify set! try
Defined symbols:
 * *command-line-args* + - / < <= = == > >= apply eval filter fold gensym instance? list load-file load-string macroexpand map mod nil? not not= nth pr print println prn quot range read read-line read-string slurp spit str symbol type
Macros:
 defn dotimes when while
```

## Examples ##
### Hello, World! ###
```
(println "Hello, World!")
```

```
(javax.swing.JOptionPane/showMessageDialog nil "Hello, World!") ; GUI Hello, World!
```

### Whitespace ###
` `, `\t`, `\r`, `\n`, `,` are whitespaces.

### Comment ###
```
; end-of-line comment
```

### Reader syntax ###
```
' quote
` quasiquote
~ unquote
~@ unquote-splicing
```

### Data types ###
You can use all Java's data types.

Literals:
```
> (doseq (x '(3 3L 3.0 3e3 true false nil "string" #"regex" \a :a () [])) (prn x ': (type x)))
3 : java.lang.Integer
3 : java.lang.Long
3.0 : java.lang.Double
3000.0 : java.lang.Double
true : java.lang.Boolean
false : java.lang.Boolean
nil : nil
"string" : java.lang.String
#"regex" : java.util.regex.Pattern
\a : java.lang.Character
:a : javelin.Keyword
() : java.util.ArrayList
[] : java.util.Vector
nil
```
* Characters - preceded by a backslash: \c. \newline, \space, \tab, \formfeed, \backspace, and \return yield the corresponding characters. Unicode characters are represented with \uNNNN as in Java. Octals are represented with \oNNN.
* nil Means 'nothing/no-value'- represents Java null and tests logical false
* Booleans - true and false

### Special form ###
```
> (let (a 1, b 2) (+ a b)) ; , is whitespace. () and [] are interchangeable in special forms.
3
> (doseq (x '(1 2 3)) (print x))
123nil
; (try EXPR ... (catch CLASS VAR EXPR ...) ... (finally EXPR ...))
> (try (quot 1 0) (catch ArithmeticException e (println e) 3) (finally (println 4)))
java.lang.ArithmeticException: / by zero
4
3
```

### Function ###

In a function, [lexical scoping](http://en.wikipedia.org/wiki/Lexical_scoping#Lexical_scoping) is used.

Functions implement java.util.concurrent.Callable, java.java.lang.Runnable and java.util.Comparator interfaces.

Callable
```
> (. * call)
1
```

Runnable
```
> (def t1 (new Thread (fn () (loop (i 1) (when (<= i 10) (print "" i) (recur (+ i 1)))))))
  (def t2 (new Thread (fn () (loop (i 11) (when (<= i 20) (print "" i) (recur (+ i 1)))))))
  (. t1 start) (. t2 start) (. t1 join) (. t2 join)
  111 12 2 13 3 14 4 15 5 16 6 17 7 18 8 19 9 20 10nil
```

Comparator
```
> (def a (list 3 2 1))
(3 2 1)
> (. java.util.Collections sort a -)
nil
> a
(1 2 3)
```

```
> ((fn (x y) (+ x y)) 1 2)
3
> ((fn (x) (* x 2)) 3)
6
> (defn foo (x & more) (list x more)) ; variadic function
(fn (x & more) (list x more))
> (foo 1 2 3 4 5)
(1 (2 3 4 5))
> (defn sum (x y) (+ x y))
(fn (x y) (+ x y))
> (sum 1 2)
3
> (fold + '(1 2 3))
6
> (defn even? (x) (== 0 (mod x 2)))
(fn (x) (== 0 (mod x 2)))
> (even? 3)
false
> (even? 4)
true
> (apply + (list 1 2 3))
6
> (map (fn (x) (. Math sqrt x)) (list 1 2 3 4))
(1.0 1.4142135623730951 1.7320508075688772 2.0)
> (filter even? (list 1 2 3 4 5))
(2 4)
> (= "abc" "abc") ; Object.equals()
true
> (def x 1)
  ((fn (x) (println x) (set! x 3) (println x)) 4) ; lexical scoping
  x
4
3
1
> (defn adder (amount) (fn (x) (+ x amount))) ; lexical scoping
  (def add3 (adder 3))
  (add3 4)
7
> (symbol "a")
a
```

#### Iterable
apply, doseq, filter, fold, map work on java.lang.Iterable.
```
> (apply + (filter (fn (x) (or (== 0 (mod x 3)) (== 0 (mod x 5)))) (range 1 1000)))
233168
```

#### Recur
Evaluates the arguments in order. Execution then jumps back to the recursion point, a loop or fn method.

Warning: `recur` does not check the tail position.
```
> (defn sum1 (n s) (if (< n 1) s (recur (- n 1) (+ s n))))
> (defn sum (n) (sum1 n 0))
> (defn sum-nonrecur (n) (if (< n 1) 0 (+ n (sum-nonrecur (- n 1)))))
> (sum 100)
5050
> (sum-nonrecur 100)
5050
> (sum 1000)
500500
> (sum-nonrecur 1000) ; stack overflow
Exception in thread "main" java.lang.StackOverflowError
> (loop (i 0) (when (< i 5) (print i) (recur (+ i 1))))
01234nil
```

### Scope ###
`doseq`, `fn`, `let`, `loop` make new scope.

### List ###
```
> (. (list 2 4 6) get 1)
4
> ((list 2 4 6) 1) ; implicit indexing
4
> (. (list 1 2 3) size)
3
```

### Array ###
```
> (. java.lang.reflect.Array get (. "a b" split " ") 1)
"b"
> ((. "a b" split " ") 1) ; implicit indexing
"b"
```

### Macro ###
Macro is non-hygienic.

```
> (defmacro infix (a op & more) `(~op ~a ~@more))
nil
> (infix 3 + 4)
7
> (infix 3 + 4 5)
12
> (macroexpand '(infix 3 + 4 5))
(+  3 4 5)
> (macroexpand '(while true (println 1)))
(loop () (if true (do (println 1) (recur))))
```

### Java interoperability (from Javelin) ###
```
> (import java.util) ; java.lang is imported by default.
nil
> (import java.util.Date) ; Clojure syntax
java.util.Date
> (new Date) (Date.)
Tue Sep 22 14:33:28 KST 2015
Tue Sep 22 14:33:28 KST 2015
> (. Math random) (Math/random) ; class's static method.
0.4780254852371699
0.21577491460068765
> (. Math floor 1.5)
1.0
> (. "abc" length) (.length "abc") ; object's method
3
3
> (. true toString)
true
> (def i 3)
3
> (. i doubleValue)
3.0
> (. Math -PI) Math/PI ; get field
3.141592653589793
3.141592653589793
> (. javelin.Core -testField)
nil
> (set! (. javelin.Core -testField) 1) ; set field
  (. javelin.Core -testField)
1
> (set! javelin.Core/testField "abc")
  javelin.Core/testField
abc
> (. (new java.math.BigInteger "2") pow 100) ; 2 ^ 100
1267650600228229401496703205376
```

#### Reify example
```
(import javax.swing java.awt java.awt.event)

(def frame (new JFrame))
(def button (new Button "Hello"))
(. button addActionListener
	(reify java.awt.event.ActionListener
	  (actionPerformed (this e)
		(. javax.swing.JOptionPane showMessageDialog nil (str "Hello, World!\nthis=" this "\ne=" e)))))
(. frame setDefaultCloseOperation (. JFrame -EXIT_ON_CLOSE))
(. frame add button (. BorderLayout -NORTH))
(. frame pack)
(. frame setVisible true)
```

#### Regular expression example
```
(defn get-title [url]
  (try
    (let [text (slurp url :encoding "utf-8")
          m (. #"(?i)<title>(.+)</title>" matcher text)]
      (if (. m find) (. m group 1) ""))
    (catch Exception e (println e))))

(println (get-title "http://www.example.com"))
```

### Java interoperability (from Java) ###
Player.java
```
public class Player {
    private int life;
    public int getLife() {
        return life;
    }
    public void setLife(int life) {
        this.life = life;
    }
}
```

InteropTest.java
```
public class InteropTest {
    public static Object testField;
    public static void main(String[] args) {
        Player pl = new Player();

        // Method 1: using class's field
        testField = pl;
        javelin.Core.load_string("(def pl (. InteropTest -testField))");
        javelin.Core.load_string("(. pl setLife 100)");
        System.out.println(javelin.Core.load_string("(. pl getLife)"));

        // Method 2: not using class's field, set variable to Java's local variable
        javelin.Core.set("p12", pl); // set
        Player pl2 = javelin.Core.get("pl2"); // get
        javelin.Core.load_string("(. pl2 setLife 200)");
        System.out.println(javelin.Core.load_string("(. pl2 getLife)"));
    }
}

```

See the source code for details.

## License ##

   Copyright 2015-2023 KIM Taegyoon

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
