
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
- ctrl+click on link -> open in new tab
- two windows on the right with L-R / U-D position
- back~forward
- flash

- drill into predicate
- rename brainteaser-assistant -> brainteaser.assistant
- explain undo
- explain bug with dates + or

- navigation gen
  :selected-spec -> sgen {spec {:name ... :value ...}}
  apply dissoc tail
  `navigation` generator
  logic for nav on BE
  navigation explain

- search
  - apropos search
  - spec inference from data
  - next step
