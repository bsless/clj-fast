# clj-fast

Library for playing around with low level Clojure code for performance reasons
given some assumptions.
Inspired by [Naked Performance (with Clojure) – Tommi Reiman](https://www.youtube.com/watch?v=3SSHjKT3ZmA).

Some of the code is based on implementations in metosin's projects. Credit in code.

## Usage

### Requirements

Add in your `project.clj`:

```clojure
[bsless/clj-fast "0.0.1-alpha"]
```

And require:

```clojure
(require '[clj-fast.core :as fast])
```

### Functions and Macros

#### Fast(er) Functions

- `fast-assoc`: Used like `assoc` but doesn't take variable key-values, only one pair.
- `fast-map` & `fast-get`: Wrappers for `java.util.HashMap`.
- `fast-map-merge`: Slightly faster version for `merge`, takes only 2 maps.

#### Inline Macros

Like regular core functions but sequence arguments must be written explicitly or `def`ed in advance:

```clojure
(def ks [:a :b])

(fast/inline-get-in m ks)

(fast/inline-get-in m [:c :d])

(fast/inline-get-some-in m [:c :d])

(fast/inline-assoc-in m [:c :d] foo)

(fast/inline-update-in m [:c :d] inc)

(fast/inline-select-keys m [:a :b :c])

(fast/inline-merge m1 m2 m3)
```


## Experimental implementations

See [results.md](doc/results.md) for experiments' benchmark results.

### Assoc

- `fast-assoc` by Metosin.
- Inlined `assoc` which expands the "rest" args. (not tested)

### Assoc in

- Inlined `assoc-in` which expands the keys sequence.

### Get

- `fast-get` by Metosin.

### Merge

- `fast-map-merge`: Metosin's implementation. Uses `kv-reduce` to `fast-assoc` all of one map into another.
- `inline-merge`: inlines core's `merge` reduction over a sequence of maps with `conj` to a nested `conj` of all maps.
- `inline-fast-map-merge`: same but with Metosin's `fast-map-merge`.

### Get in

- `inline-get-in`: given that all keys are written as explicit arguments and not a sequence, `get-in` can be expanded into a series of `get`s.
- `inline-get-some-in`: same as above, but maps can be invoked on the keys. nil checks every iteration.

### Assoc in

- `inline-assoc-in`: same as `inline-get-in` but with `assoc-in`.

### Assoc in

- `inline-update-in`: same as `inline-assoc-in` but with `update-in`.

### select-keys

- `inline-select-keys`: same case with `get-in` can be done with
`select-keys`.

## License

Copyright © 2019 ben.sless@gmail.com

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

## Credit

Credit to Metosin wherever noted in the code.
