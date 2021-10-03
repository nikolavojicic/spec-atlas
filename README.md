
### Development mode
```
npm install
npx shadow-cljs watch app
```
start a ClojureScript REPL
```
npx shadow-cljs browser-repl
```
### Building for production

```
npx shadow-cljs release app
```
### TODO

- Alt+Click to (un)fold all
- two windows on the right with L-R / U-D position
- back~forward
- drill into predicate + support lambdas in s/def
- rename brainteaser-assistant -> brainteaser.assistant
- search
  - apropos
  - inference
  - next step
- improve explain (bug with dates + or, undo)
- flash
- generative testing

- navigation gen
  :selected-spec -> sgen {spec {:name ... :value ...}}
  apply dissoc tail
  `navigation` generator
  logic for nav on BE
  navigation explain

- better fspec - URI
- exercise


