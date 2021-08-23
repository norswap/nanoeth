# Ethereum History

This package manages Ethereum's "versions", i.e. the various hard forks that were rolled out.

These hard forks changed the specification, sometimes adding to it, and sometimes making backward
incompatible changes.

Note that the current version of the yellow paper targets the Istanbul hard fork.

The state of this package is in flux. My current thinking is that it is better to specify
"backward-compatibility" behaviour (behaviour that must still be supported by the current version
of the chain, if only to be able to process old blocks) in the relevant components.

So it's possible that this package will just include the list of versions in `EthereumVersion`
and some historical context in documentation files such as this one.

## Per-Version Info

Top link is the blog post announcing the version, sub-links include the hard-fork meta EIP and/or
specification, and the list of included EIPs.

Also check the [similar page on the ethereum/execution-specs repo](https://github.com/ethereum/execution-specs/blob/master/README.md)

- [Frontier](https://blog.ethereum.org/2015/07/22/frontier-is-coming-what-to-expect-and-how-to-prepare/)
    - mainnet: 30 July 2015

- [Frontier Thawing](https://blog.ethereum.org/2015/08/04/the-thawing-frontier/)
    - mainnet: 08 September 2015

- [Homestead](https://blog.ethereum.org/2016/02/29/homestead-release/)
  - [Meta (EIP-606)](https://eips.ethereum.org/EIPS/eip-606),
    [EIP-2](https://eips.ethereum.org/EIPS/eip-2),
    [EIP-7](https://eips.ethereum.org/EIPS/eip-7),
    [EIP-8](https://eips.ethereum.org/EIPS/eip-8)
  - mainnet: 15 March 2016

- [DAO Fork](https://blog.ethereum.org/2016/07/20/hard-fork-completed/)
  - [Meta (EIP-779)](https://eips.ethereum.org/EIPS/eip-779)
  - mainnet: 20 July 2016

- [Tangerine Whistle](https://blog.ethereum.org/2016/10/18/faq-upcoming-ethereum-hard-fork/)
  - [Meta (EIP-608)](https://eips.ethereum.org/EIPS/eip-608),
    [EIP-150](https://eips.ethereum.org/EIPS/eip-150)
  - mainnet: 18 October 2016

- [Spurious Dragon](https://blog.ethereum.org/2016/11/18/hard-fork-no-4-spurious-dragon/)
  - [Meta (EIP-607)](https://eips.ethereum.org/EIPS/eip-607),
    [EIP-155](https://eips.ethereum.org/EIPS//155),
    [EIP-160](https://eips.ethereum.org/EIPS//160),
    [EIP-161](https://eips.ethereum.org/EIPS//161),
    [EIP-170](https://eips.ethereum.org/EIPS//170)
  - mainnet: 23 November 2016

- [Byzantium](https://blog.ethereum.org/2017/10/12/byzantium-hf-announcement/)
  - [Meta (EIP-609)](https://eips.ethereum.org/EIPS/eip-609) ,
    [EIP-100](https://eips.ethereum.org/EIPS/100),
    [EIP-140](https://eips.ethereum.org/EIPS/140),
    [EIP-196](https://eips.ethereum.org/EIPS/196),
    [EIP-197](https://eips.ethereum.org/EIPS/197),
    [EIP-198](https://eips.ethereum.org/EIPS/198),
    [EIP-211](https://eips.ethereum.org/EIPS/211),
    [EIP-214](https://eips.ethereum.org/EIPS/214),
    [EIP-649](https://eips.ethereum.org/EIPS/649),
    [EIP-658](https://eips.ethereum.org/EIPS/658)
  - mainnet: 16 October 2017

- [Constantinople](https://blog.ethereum.org/2019/02/22/ethereum-constantinople-st-petersburg-upgrade-announcement/)
  - [Meta (EIP-1013)](https://eips.ethereum.org/EIPS/eip-1013),
    [EIP-145](https://eips.ethereum.org/EIPS/eip-145),
    [EIP-1014](https://eips.ethereum.org/EIPS/eip-1014),
    [EIP-1052](https://eips.ethereum.org/EIPS/eip-1052),
    [EIP-1234](https://eips.ethereum.org/EIPS/eip-1234),
    [EIP-1283](https://eips.ethereum.org/EIPS/eip-1283)
  - mainnet: 28 February 2019

- [St-Petersburg](https://blog.ethereum.org/2019/02/22/ethereum-constantinople-st-petersburg-upgrade-announcement/)
  - [Meta (EIP-1716)](https://eips.ethereum.org/EIPS/eip-1716),
    REMOVED [EIP-1283](https://eips.ethereum.org/EIPS/eip-1283)
  - Note: on same height as Constantinople on mainnet, leading to EIP-1283 never hitting mainnet.
    Constantinople is earlier on testnets.
  - mainnet: 28 February 2019

- [Istanbul](https://blog.ethereum.org/2019/11/20/ethereum-istanbul-upgrade-announcement/)
  - [Meta (EIP-1679)](https://eips.ethereum.org/EIPS/eip-1679),
    [EIP-152](https://eips.ethereum.org/EIPS/eip-152),
    [EIP-1108](https://eips.ethereum.org/EIPS/eip-1108),
    [EIP-1344](https://eips.ethereum.org/EIPS/eip-1344),
    [EIP-1884](https://eips.ethereum.org/EIPS/eip-1884),
    [EIP-2028](https://eips.ethereum.org/EIPS/eip-2028),
    [EIP-2200](https://eips.ethereum.org/EIPS/eip-2200)
  - mainnet: 08 December 2019

- [Muir Glacier](https://blog.ethereum.org/2019/12/23/ethereum-muir-glacier-upgrade-announcement/)
  - [Meta (EIP-2387)](https://eips.ethereum.org/EIPS/eip-2387),
    [EIP-2384](https://eips.ethereum.org/EIPS/eip-2384)
  - mainnet: 01 January 2020

- [Berlin](https://blog.ethereum.org/2021/03/08/ethereum-berlin-upgrade-announcement/)
  - [Meta (EIP-2070)](https://eips.ethereum.org/EIPS/eip-2070),
    [Specification](https://github.com/ethereum/eth1.0-specs/blob/master/network-upgrades/mainnet-upgrades/berlin.md) ,
    [EIP-2565](https://eips.ethereum.org/EIPS/eip-2565),
    [EIP-2929](https://eips.ethereum.org/EIPS/eip-2929),
    [EIP-2718](https://eips.ethereum.org/EIPS/eip-2718),
    [EIP-2930](https://eips.ethereum.org/EIPS/eip-2930)
  - mainnet: 04 April 2021

- [London](https://blog.ethereum.org/2021/07/15/london-mainnet-announcement/)
  - [Specification](https://github.com/ethereum/eth1.0-specs/blob/master/network-upgrades/mainnet-upgrades/london.md),
    [EIP-1559](https://eips.ethereum.org/EIPS/eip-1559),
    [EIP-3198](https://eips.ethereum.org/EIPS/eip-3198),
    [EIP-3529](https://eips.ethereum.org/EIPS/eip-3529),
    [EIP-3541](https://eips.ethereum.org/EIPS/eip-3541),
    [EIP-3554](https://eips.ethereum.org/EIPS/eip-3554)
  - mainnet: 04 August 2021
