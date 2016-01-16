# The Javelin Programming Language

(C) 2015 KIM Taegyoon

Javelin is a dialect of Lisp. It is designed to be an embedded language (minimal Lisp for the Java Virtual Machine).

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
 . .get .set! and catch def defmacro do doseq finally fn if import let loop new or quasiquote quote recur reify set! try
Defined symbols:
 * *command-line-args* + - / < <= = == > >= apply eval filter fold gensym list macroexpand map mod nil? not not= pr print println prn quot read read-line read-string slurp spit str symbol type
Macros:
 defn dotimes when while
```

## Examples ##
### Hello, World! ###
```
(println "Hello, World!")
```

```
(. javax.swing.JOptionPane showMessageDialog nil "Hello, World!") ; GUI Hello, World!
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
> (map type '(3 3L 3.0 3e3 true false nil "string" \a))
(java.lang.Integer java.lang.Long java.lang.Double java.lang.Double java.lang.Boolean java.lang.Boolean nil java.lang.String java.lang.Character)
```
* Characters - preceded by a backslash: \c. \newline, \space, \tab, \formfeed, \backspace, and \return yield the corresponding characters. Unicode characters are represented with \uNNNN as in Java. Octals are represented with \oNNN.
* nil Means 'nothing/no-value'- represents Java null and tests logical false
* Booleans - true and false

### Special form ###
```
> (let (a 1, b 2) (+ a b)) ; , is whitespace.
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
`doseq`, `fn`, `let`, `loop`, `thread` make new scope.

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
> (import) ; shows current import list. java.lang is imported by default. Classes are found in this order.
("java.lang")
> (import java.util)
("java.lang" "java.util")
> (new Date)
Tue Sep 22 14:33:28 KST 2015
> (. Math random) ; class's static method.
0.4780254852371699
> (. Math floor 1.5)
1.0
> (. "abc" length) ; object's method
3
> (. true toString)
true
> (def i 3)
3
> (. i doubleValue)
3.0
> (.get Math PI) ; get field
3.141592653589793
> (.get javelin.Core testField)(.get Core testField)
nil
> (.set! javelin.Core testField 1) ; set field
  (.get javelin.Core testField)
1
> (.set! javelin.Core testField "abc")
  (.get javelin.Core testField)
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
(. frame setDefaultCloseOperation (.get JFrame EXIT_ON_CLOSE))
(. frame add button (.get BorderLayout NORTH))
(. frame pack)
(. frame setVisible true)
```

#### KOSPI200 Ticker
```
; KOSPI200 Ticker (C) 2015 KIM Taegyoon
(import java.util)

(def p (. regex.Pattern compile "KOSPI200.*>(.+)</font>"))
(defn get-quote ()
  (try
    (def text (slurp "http://kosdb.koscom.co.kr/main/jisuticker.html" "euc-kr"))
    (def m (. p matcher text))
    (if (. m find) (. (. m group 1) replaceAll "&nbsp;" "") "")
  (catch Exception e (println e))))

(loop ()
  (println (new Date) ":" (get-quote))
  (. Thread sleep 2000)
  (recur))
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
        javelin.Core j = new javelin.Core();
        Player pl = new Player();

        // Method 1: using class's field
        testField = pl;
        j.evalString("(def pl (.get InteropTest testField))");
        j.evalString("(. pl setLife 100)");
        System.out.println(j.evalString("(. pl getLife)"));

        // Method 2: not using class's field, set variable to Java's local variable
        j.set("p12", pl); // set
        Player pl2 = j.get("pl2"); // get
        j.evalString("(. pl2 setLife 200)");
        System.out.println(j.evalString("(. pl2 getLife)"));
    }
}

```

See the source code for details.

## License ##

   Copyright 2015 KIM Taegyoon

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
