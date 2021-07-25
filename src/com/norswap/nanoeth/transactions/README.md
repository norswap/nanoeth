# Transactions

TODO talk about normalized transactions

The transaction format underwent multiple changes as Ethereum evolved.
The `TransactionFormat` enum lists all possible format for a transaction.

These include:

- The original format, notable by the absence of a chain identifier. This enabled replay attack on
  forks of Ethereum (most notably Ethereum Classic after the DAO fork).
  
- The [EIP-155] format (Spurious Dragon hard fork), which adds the chain identifier. The old format
  is still supported (if only to be able to process transactions already on-chain). New transactions
  should use a format that includes a chain ID (all excepted the legacy format), but that isn't
  enforced. As of March 2021, the Geth team [has announced][chainid-enforcement] they would
  gradually start enforcing the presence of a chain ID.

- The [EIP-2930] format (Berlin hard fork) which adds access lists to transaction, allowing to
  specify in advance addresses and storage keys that the transaction will access. This is an optional
  transaction type (as specified by [EIP-2718]).

- The [EIP-1559] format (London hard fork), which deprecates the gas price in favour of "max fee per
  gas" and "max priority fee per gas" (the latter being included in the former). This is an optional
  transaction type (as specified by [EIP-2718]). In case the older formats are used, the EVM acts as
  though "max fee per gas" and "max priority fee per gas" had been set to the gas price.

[EIP-155]: https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md
[EIP-2930]: https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2930.md
[EIP-1559]: https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md
[EIP-2718]: https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2718.md
[chainid-enforcement]: https://blog.ethereum.org/2021/03/03/geth-v1-10-0/#chainid-enforcement

As noted, there are currently two "typed transactions", introduced by [EIP-2930] and [EIP-1559]. The
pain was felt in EIP-155, where the chain ID is packed into the v signature (in the transaction's
serialization in a block). The answer was [EIP-2718]'s '"typed transaction envelopes" which
designate the transaction type <= 127 (0x7F) by prepending a byte to the (RLP-encoded) transaction
payload. This works because transaction are sequences, and their [RLP-encoding](../rlp/README.md)
therefore always starts with a byte that is > 192 (0xC0).

I chose to use the term "format" to englobe both typed transactions, and the two "formats" that
precede them. Beyond the data fields making up a transaction, different format also entail signing
differences, and difference in the format of receipts.