# axie

This interacts with the Axie Infinity API.

You can use it to look for axies for sale in a way that's not possible
through their filter UI. For example, you can sort in descending order
by attack, defense, attack+defense, each individual stat, purity, and at
the same time you can break ties by sorting in ascending order by price.
That makes it easier to find axies that are a good deal.

You can also determine how much longer until your teams are ready for battle,
you can send a single team to battle, or you can send all the teams that are
ready off to battle at once.

## Usage

For now, it can only be used in the REPL.

## Options

It needs to read a config file, called `.axie.conf` in the working directory.

The config file needs to contain your Ethereum address and your Axie Infinity
bearer token (which can be learned from local storage on their website, at
the key `axieinfinity.<eth-addr>.auth`).

```
{:eth-addr "<your-eth-address>"
 :token "<your-bearer-token>"}
 ```

## Examples

Find some interesting axies to buy:

```
(def all @(fetch-all))

(->> all (sort-axies [:atk+def :desc] [:price :asc]) (map search-keys) dedupe (take 4) print-table)
```

Look at some of your own axies:

```
(def mine @(fetch-my-axies))

(->> mine (sort-axies [:atk+def :desc]) (map mine-keys) (take 4) print-table)
```

Check out your teams:

```
@(md/chain (fetch-teams) print-table)
```

Start all the battles:

```
@(start-battles)
```

## License

Copyright © 2019 Sean Schulte

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
