# Receipts

## Log Topics & Bloom Filters

A bloom filter is a space-efficient index datas tructure (approximating a set) that can have false
positive (it can say that the set contains an entry when it doesn't) but no false negatives (if it
says the set does not contain an entry, then the set really doesn't contain it).

In Ethereum, bloom filters are used to index log entries in a single block. In particular, each
block header contains a bloom filter, which can be queried to know whether the block may contain log
entries with a given topic, or created by a given address.

While the yellowpaper makes it seem like a bloom filter is generated for every single log entry, in
reality, only a single bloom filter is generated for all log entries from the same block
(this bloom filter is included in the block header). However that bloom filter can be seen as a
bitwise-or combination of all single-log-entry bloom filters. Interestingly, the yellowpaper does
not given an explicit equation for the block header logs bloom filter, thought it's easy to
understand what is meant.

The log topics are used by clients to monitor events of interesting. They are the mechanism through
which Solidity's events are implemented.

If a client wants to process all logs with a certain topic, the bloom filter will help him skip the
blocks that do not contain the topic, and hence lower network congestion as he will not have to
request the logs over the network. (This is not useful if you're validating the network, in which
case you have to derive the logs to validate the header anyway.)