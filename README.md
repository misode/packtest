# PackTest
PackTest allows you to write game tests in a data pack. Tests are `*.mcfunction` files in a `tests` folder. They can be used to test custom data packs.

## Example
**`data/example/tests/foo.mcfunction`**
```mcfunction
# Summons an armor stand and finds it
# @template example:small_platform
# @optional

summon armor_stand ~1.5 ~1 ~1.5
execute positioned ~1.5 ~1 ~1.5 run assert entity @e[type=armor_stand,dx=0]

assert predicate example:test

setblock ~1 ~1 ~1 grass_block
execute if block ~1 ~1 ~1 stone run succeed

fail "Oh no"
```

## Running tests
Tests can be run in-game using the `test` command.
* `test runall`: runs all the tests
* `test runall <namespace>`: runs all tests from a specified namespace
* `test run <test>`: runs the test with a specified name
* `test runfailed`: runs all the previously failed tests
* `test runthis`: runs the closes test
* `test runthese`: runs all tests within 200 blocks

## Auto test server
Tests can also be run automatically, for instance in a CI environment. When `-Dpacktest.auto` is set, the game test server will start automatically with the loaded tests. The process will exit when all tests have finished with the exist code set to the number of failed tests. 

## Commands

### `fail`
* `fail <text component>`: fails the current test and returns from the function

### `succeed`
* `succeed`: always succeeds the current test and returns from the function
* `succeed when <condition>`: succeeds when the condition is successful, tries every tick until the test times out
* `succeed when not <condition>`: succeeds when the condition is unsuccessful, tries every tick until the test times out

### `assert`
* `assert <condition>`: if condition is unsuccessful, fails the current test and returns from the function
* `assert not <condition>`: if condition is successful, fails the current test and returns from the function

## Conditions
* `block <pos> <block>`: checks if the block at the specified position matches the block predicate
* `entity <selector>`: checks if the selector matches any entity (can also find entities outside the structure bounds)
* `predicate <predicate>`: checks a predicate in a data pack
* `score ...`: checks scores using the same syntax as `execute if score`

## Directives
Tests can be customized by placing certain directives as special comments at the start of the test function.

* `@template`: the resource location of a structure template to use for the test, defaults to an empty 1x1x1 structure
* `@batch`: the batch name for this test, defaults to `packtestBatch`
* `@timeout`: an integer specifying the timeout, defaults to `100`
* `@optional`: whether this test is allowed to fail, defaults to `false`, if there is no value after the directive it is considered as `true`
