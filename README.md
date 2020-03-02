<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
  <p></p>
  <img src="https://www.bootcampbaltic.com/static2/ico/og_image_social.png" alt="Nasdaq">
</p>


First exercises are a basic example to create, issue and perform a DvP (Delivery vs Payment) of an Evolvable NonFungible token in Corda utilizing the TokenSDK. 

Second exercises are for utilizing accounts for transactions with Tokens.

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Materials

All slide decks and Corda Design Language (CDL) diagrams can be found in the `resources` folder

# Usage

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes

### Shell Commands for executing DVP with TokenSDK

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.
    
    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.

First go to the shell of PartyA and issue some USD to Party C. We will need the fiat currency to exchange it for the asset token. 

    start FiatCurrencyIssueFlow currency: USD, amount: 100000000, recipient: PartyC

We can now go to the shell of PartyC and check the amount of USD issued. Since fiat currency is a fungible token we can query the vault for FungibleToken states.

    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    
Once we have the USD issued to PartyC, we can Create and Issue the AssetToken to PartyB. Goto PartyA's shell to create and issue the asset token.
    
    start AssetTokenCreateAndIssueFlow owner: PartyB, name: "awesome asset", value: 10000 USD
    
We can now check the issued asset token in PartyB's vault. Since we issued it as a non-fungible token we can query the vault for non-fungible tokens.
    
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
    
Note that asset token is an evolveable token which is a linear state, thus we can check PartyB's vault to view the evolveable token

    run vaultQuery contractStateType: com.bootcamp.day2.states.AssetTokenState
    
Note the linearId of the asset token from the previous step, we will need it to perform our DvP operation. Goto PartyB's shell to initiate the token sale.
    
    start AssetTokenSaleInitiatorFlow assetStateId: <XXXX-XXXX-XXXX-XXXXX>, buyer: PartyC
    
We could now verify that the non-fungible token has been transferred to PartyC and some 100,000 USD from PartyC's vault has been transferred to PartyB. Run the below commands in PartyB and PartyC's shell to verify the same
    
    // Run on PartyB's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    // Run on PartyC's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken

---

### Shell Commands for Transacting between Accounts

Accounts functionality is implemented at the Node level through an Accounts CorDapp. 

####Creating accounts on a node
We can create accounts by executing a flow.
Goto PartyA's shell and create an account for 'Bob'

``
start CreateAccount name: "PartyA - Bob"
``

Create another account on PartyB's shell

``
start CreateAccount name: "PartyB - Alice"
``


#### Checking accounts on a node
We can see what accounts are on a node with the flow OurAccounts. 
Execute this on PartyA to see 'Bob' and on PartyB to see 'Alice'

``
start OurAccounts
``

#### Share an account on our node with another party

From PartyB

``
start ShareAccountToParty acctName: PartyB - Alice, shareTo: PartyA
``

#### Self Issue our AssetForAccountState

From PartyA

``
start AssetForAccountStateSelfIssueFlow acctName: PartyA - Bob, name: "diamond", value: 100
``

#### Transfer our Issued state from our Account to an Account on another Node

From PartyA

Do a vaultQuery to get the transaction hash and index of the AssetForAccountState

``
run vaultQuery contractStateType: AssetForAccountState
``

Then run the same query from PartyB - notice the vault is empty.

Go back to PartyA

Copy the transaction hash from your query and use as an argument for the transfer flow

``
start NodeAccountToNodeAccount senderName: PartyA - Bob, receiverName: PartyB - Alice, hash: <txhash>, index: 0
``

run the vaultQueries again and see that the asset has changed hands, the owner key now
maps to PartyB - Alice account.

``
run vaultQuery contractStateType: AssetForAccountState
``