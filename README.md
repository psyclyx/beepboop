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
And then, to start or reload
```clojure
(refresh)
```

### Run
```shell
clj -M -m beepboop.main
```

### Client Connection
```shell
telnet localhost 9090
```
Or, to reconnect across refreshes
```shell
while telnet localhost 9090; do sleep 0.5; echo reconnecting...; done
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
