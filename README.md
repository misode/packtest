# PackTest
PackTest allows you to write game tests in a data pack. Tests are `*.mcfunction` files in a `test` folder. They can be used to test custom data packs.

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/packtest)

## Example
**`data/example/test/foo.mcfunction`**
```mcfunction
#> Summons an armor stand and finds it
# @template example:small_platform
# @optional

summon armor_stand ~1.5 ~1 ~1.5
execute positioned ~1.5 ~1 ~1.5 run assert entity @e[type=armor_stand,dx=0]

assert predicate example:test

setblock ~1 ~1 ~1 grass_block
execute if block ~1 ~1 ~1 stone run succeed

fail "Oh no"
```

### Async tests
Test functions can be asynchronous, using the `await` keyword!
```mcfunction
setblock ~ ~ ~ stone
summon item ~ ~6 ~

await entity @e[type=item,distance=..2]

await delay 1s

data merge entity @e[type=item,distance=..2,limit=1] {Motion: [0.0, 0.01, 0.0]}
```

## Running tests
Tests can be run in-game using the `test` command. Some example commands:
* `test run *:*`: runs all the tests
* `test run <namespace>:*`: runs all tests from a specified namespace
* `test run <test>`: runs the test with a specified name
* `test runfailed`: runs all the previously failed tests
* `test runthis`: runs the closes test
* `test runthese`: runs all tests within 200 blocks

### Auto test server
Tests can also be run automatically, for instance in a CI environment. When `-Dpacktest.auto` is set, the game test server will start automatically with the loaded tests. The process will exit when all tests have finished with the exist code set to the number of failed tests. 

Setting `-Dpacktest.auto.annotations` will emit GitHub annotations for all test failures and resource load errors.

The following example can be adapted into a GitHub action workflow.
```yaml
on: [push, pull_request]

env:
  # Make sure to update these links!
  TEST_FABRIC_SERVER: https://meta.fabricmc.net/v2/versions/loader/1.20.4/0.15.3/0.11.2/server/jar
  TEST_FABRIC_API: https://cdn.modrinth.com/data/P7dR8mSH/versions/JMCwDuki/fabric-api-0.92.0%2B1.20.4.jar
  TEST_PACKTEST: https://cdn.modrinth.com/data/XsKUhp45/versions/18smpIeE/packtest-1.6-mc1.20.4.jar

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Download and prepare files
        run: |
          curl -o server.jar $TEST_FABRIC_SERVER
          mkdir mods
          curl -o mods/fabric-api.jar $TEST_FABRIC_API
          curl -o mods/packtest.jar $TEST_PACKTEST
          mkdir -p world/datapacks
          cp -r datapack world/datapacks/datapack
      - name: Run tests
        run: |
          java -Xmx2G -Dpacktest.auto -Dpacktest.auto.annotations -jar server.jar nogui
```

## Commands

### `fail`
* `fail <text component>`: fails the current test and returns from the function

### `succeed`
* `succeed`: always succeeds the current test and returns from the function

### `assert`
* `assert <condition>`: if condition is unsuccessful, fails the current test and returns from the function
* `assert not <condition>`: if condition is successful, fails the current test and returns from the function

### `await`
* `await <condition>`: similar to assert, but keeps trying the condition every tick until the test times our or the condition succeeds
* `await not <condition>`: keeps trying the condition until it fails
* `await delay <time>`: waits for a specified time with unit

### Conditions
* `block <pos> <block>`: checks if the block at the specified position matches the block predicate
* `data ...`: checks NBT data using the same syntax as `execute if score`
* `entity <selector>`: checks if the selector matches any entity (can also find entities outside the structure bounds)
* `predicate <predicate>`: checks a predicate in a data pack
* `score ...`: checks scores using the same syntax as `execute if score`
* `chat <pattern> [<receivers>]`: checks whether a chat message was sent in the past tick matching a regex pattern

## Dummies
Fake players can be spawned using the `/dummy` command. Dummies won't save or load their data from disk, they will also not load their skin.

* `dummy <name> spawn`: spawns a new dummy
* `dummy <name> respawn`: respawns the dummy after it has been killed
* `dummy <name> leave`: makes the dummy leave the server
* `dummy <name> jump`: makes the dummy jump, if currently on ground
* `dummy <name> sneak [true|false]`: makes the dummy hold shift or un-shift (not the same as currently crouching)
* `dummy <name> sprint [true|false]`: makes the dummy sprint or un-sprint
* `dummy <name> drop [all]`: makes the dummy drop the current mainhand, either one item or the entire stack
* `dummy <name> swap`: makes the dummy swap its mainhand and offhand
* `dummy <name> selectslot`: makes the dummy select a different hotbar slot
* `dummy <name> use item`: makes the dummy use its hand item, either mainhand or offhand
* `dummy <name> use block <pos> [<direction>]`: makes the dummy use its hand item on a block position
* `dummy <name> use entity <entity>`: makes the dummy use its hand item on an entity
* `dummy <name> attack <entity>`: makes the dummy attack an entity with its mainhand
* `dummy <name> mine <pos>`: makes the dummy mine a block

## Directives
Tests can be customized by placing certain directives as special comments at the start of the test function.

* `@environment`: the test environment for this test, defaults to `minecraft:default`
* `@template`: the resource location of a structure template to use for the test, defaults to `minecraft:empty`, which is an empty 1x1x1 structure
* `@timeout`: an integer specifying the timeout, defaults to `100`
* `@optional`: whether this test is allowed to fail, defaults to `false`, if there is no value after the directive it is considered as `true`
* `@skyaccess`: whether this test needs sky access, defaults to `false`, which will place barrier blocks above the test
* `@dummy`: whether to spawn a dummy at the start of the test and set `@s` to this dummy, taking a position which defaults to `~0.5 ~ ~0.5`
