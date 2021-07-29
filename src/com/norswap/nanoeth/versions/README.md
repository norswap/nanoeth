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

## Version Announcement Blog Posts

I couldn't pinpoint specific blog posts for Frontier (initial release) & Ice Age, but I didn't
look really hard. Feel free to suggest something.

- Frontier
- Ice Age  
- [Homestead](https://blog.ethereum.org/2016/02/29/homestead-release/)
- [DAO Fork](https://blog.ethereum.org/2016/07/20/hard-fork-completed/)
- [Tangerine Whistle](https://blog.ethereum.org/2016/10/18/faq-upcoming-ethereum-hard-fork/)  
- [Spurious Dragon](https://blog.ethereum.org/2016/11/18/hard-fork-no-4-spurious-dragon/)
- [Byzantium](https://blog.ethereum.org/2017/10/12/byzantium-hf-announcement/)
- [Constantinople / St-Petersburg](https://blog.ethereum.org/2019/02/22/ethereum-constantinople-st-petersburg-upgrade-announcement/)
- [Istanbul](https://blog.ethereum.org/2019/11/20/ethereum-istanbul-upgrade-announcement/)
- [Muir Glacier](https://blog.ethereum.org/2019/12/23/ethereum-muir-glacier-upgrade-announcement/)  
- [Berlin](https://blog.ethereum.org/2021/03/08/ethereum-berlin-upgrade-announcement/)
- [London](https://blog.ethereum.org/2021/07/15/london-mainnet-announcement/)
