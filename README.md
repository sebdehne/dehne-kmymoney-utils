# KMyMoney Utils
A kotlin/Java library to read and write [KMyMoney](https://kmymoney.org/) database files

It also contains some code to perform bulk-edits, currently the 
following transformers are supported:

- *PaymentsToTransfersTransformer* - finds matching pairs of payment transactions
  in two different ledgers/accounts and converts them to become *transfer*-transactions
  instead
- *MoveAccountToANewParentTransformer* - Moves accounts in the hierarchy, including
  bank-account. For example convert a Asset-account to a Liability-account
- *TransactionAccountTransformer* - lets modify the account of a transaction, including
  changing the "Opening Balances"-account.


