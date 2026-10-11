# Week 2: test driven development with a kata

This week you build one small class, test first, one rule at a time. The class
has nothing to do with ferries on purpose: with no application to understand,
all of your attention goes on the rhythm of test driven development.

## What arrives this week

| File | What it is |
|---|---|
| `src/main/java/mt/edu/um/cps3230/ferry/kata/StringCalculator.java` | The class you will build. It has one method, `add`, which throws for now |
| `practicals/week-02.md` | This sheet |

Bring your repository up to date first:

```bash
git pull
mvn verify
```

The build is still green: nothing tests `StringCalculator` yet.

## The rhythm

Every rule below goes through the same three steps.

1. **Red.** Write one test for the next rule. Run it and watch it fail. A test
   you have never seen fail proves nothing.
2. **Green.** Write the least production code that makes it pass. Hard coding
   a return value is allowed, and is often the right move early on.
3. **Refactor.** With every test green, tidy the production code and the tests.
   Run the tests again after each change.

Commit after every green step. Your history then shows the kata one rule at a
time, which is useful when you come back to it.

Put your tests in `src/test/java/mt/edu/um/cps3230/ferry/kata/StringCalculatorTest.java`.
Name each test `should...When...`, as the rest of the repository does. To run
only that class while you work:

```bash
mvn test -Dtest=StringCalculatorTest
```

## The rules

Take them in order and do not read ahead: the point is to let the design grow
from the tests, not to plan it up front. Only the numbers matter for each
rule; you never have to handle input that the rules do not describe.

1. `add("")` returns `0`. `add("1")` returns `1`. `add("1,2")` returns `3`.
2. `add` handles any amount of numbers: `add("1,2,3,4,5")` returns `15`.
3. A new line also separates numbers: `add("1\n2,3")` returns `6`.
4. The string may start with a line that names a different separator, in the
   form `//;\n`. So `add("//;\n1;2")` returns `3`. Commas and new lines still
   work when there is no such line.
5. A negative number is refused. `add` throws an `IllegalArgumentException`
   whose message lists every negative number in the string, not just the
   first, for example `negatives not allowed: -1, -3`.
6. A number bigger than 1000 is left out of the sum: `add("2,1001")` returns
   `2`. A thousand itself still counts.
7. A separator can be longer than one character when it is written in square
   brackets: `add("//[***]\n1***2***3")` returns `6`.
8. There can be more than one separator, each in its own brackets:
   `add("//[*][%]\n1*2%3")` returns `6`.
9. Rules 7 and 8 together: `add("//[**][%%]\n1**2%%3")` returns `6`.

Rules 7 and 8 use characters that mean something to a regular expression. If
you split the string with `String.split`, a test with `*` in the separator is
worth writing before you find out the hard way.

## Things to notice

- When did the code first need a loop, and which test forced it?
- Which refactorings did you do on the tests themselves? A parameterised test
  (`@ParameterizedTest` with `@CsvSource`) often replaces several near
  identical tests once a rule settles.
- Did any test pass the moment you wrote it? If so, was it testing anything
  the earlier tests had not?

## Self check

The self check runs a reference suite against your `StringCalculator`:

```bash
mvn verify -Pcheck -Dweek=02
```

It is green once every rule above holds. A rule you have not reached yet shows
up as a failing test named after that rule, so you can run it at any point to
see where you are. The reference suite checks your class, not your tests: the
tests are for you, and they are what you will measure in week 6.
