REFAL-5 Interpreter [1]
----
### Build
```shell
./gradlew buildInt
```

### Run
```shell
java -jar ref5.jar <path-to-file-with-program>
```

### Examples
 There are some examples in the "ref5-examples" folder:
 * `fib.ref5` - calculates n-th fibonacci number
 * `call.ref5` - call built-in function with give args(e.g. `'Add' 2 3` or `'Mu' 'Add' 2 3`)
 * `rev.ref5` - reverses given expression
 * `pal.ref5` - checks if the given expression is palindrome
 * `seq.ref5` - checks if the given sequence of A and B is a correct braces sequence.
 * `prime.ref5` - prints all prime numbers until the given number

### Tests
```shell
./gradlew test
```

### Important notes
* with not implemented
* only these built-in functions are implemented: Add, Sub, Mul, Div, Mu, Card(input), Print, Prout(the same as Print)
* only valid refal-5 expression can be entered after the `Card` call. (e.g. `'abacaba' 2 3 4` is valid and `abacaba 2 3 4` is not)
* after each pattern-matching `;` should be put
* no `;` after function declarations

### References
* [1] Refal-5 documentation: http://refal.ru/rf5_frm.htm