# nanoeth's Limitations

- Limited to handling byte sequences of less than `2^31 - 8` (a conservative approximation of the
  max Java array size). In theory, Ethereum should handle byte arrays up to size 2^64. In pratice,
  real clients don't do much better, and it would be too costly to pay for storage greater than the
  max Java array size.

- nanoeth implements Ethereum's execution layer, and so does not participate in all of Ethereum's
  networking protocol (gossip, sync, etc). It is planned to let nanoeth get data from another
  running client, but this is not yet implement.