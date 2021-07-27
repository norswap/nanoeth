# Test Data

Imported from https://github.com/ethereum/tests at commit
`e7b5779da2f0fcf0723f83ec602795b7dff67f4c`.

- [Transaction Tests](https://ethereum-tests.readthedocs.io/en/latest/test_types/transaction_tests.html)
  
These assume that you validate:
- The format of the transaction
- The validity of the signature
- [That chainId == 1 (main net)](https://github.com/ethereum/tests/issues/584#issuecomment-887475216)
- That the gas limit is less than the gas cost
  (For transaction that don't access any state)
      
Note that there are no test cases for typed transaction envelopes (see transaction package README).