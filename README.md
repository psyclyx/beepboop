# beepboop
WIP networked terminal artillary game.

## Project status
Just getting started, unplayable. Come back later.

## Development

### REPL

Start a REPL with the `:dev` profile.

#### Emacs

This repository includes a `.dir-locals.el` that should make `cider-jack-in` Just Work.

#### CLI

```shell
clj -A:dev
```

### Run
```shell
clj -M -m beepboop.main
```

### Test
```shell
clj -M:test
```

### Utilities

#### Antq (outdated dependencies)

##### Check

```shell
clj -M:outdated
```
##### Upgrade

```shell
clj -M:outdated --upgrade
```

#### cljstyle (code formatter)

##### Check
```shell
clj -M:cljstyle check
```

##### Fix
```shell
clj -M:cljstyle fix
```
