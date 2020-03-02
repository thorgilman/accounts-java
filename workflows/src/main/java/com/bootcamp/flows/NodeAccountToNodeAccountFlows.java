package com.bootcamp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.bootcamp.contracts.AssetForAccountContract;
import com.bootcamp.contracts.AssetForAccountState;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByKey;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class NodeAccountToNodeAccountFlows {

    @InitiatingFlow
    @StartableByRPC
    public static class NodeAccountToNodeAccountInitiatorFlow extends FlowLogic<SignedTransaction> {

        private final String myName;
        private final String receiverName;
        private final StateRef assetRef;

        public NodeAccountToNodeAccountInitiatorFlow(String senderName, String receiverName, String hash, int index) {
            this.myName = senderName;
            this.receiverName = receiverName;
            this.assetRef = new StateRef(SecureHash.parse(hash), index);
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // get notary
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Get the AccountInfo for our account and generate a new key
            AccountInfo myAccount = subFlow(new AccountInfoByName(myName)).get(0)
                    .getState().getData();

            // Get the account info for the
            AccountInfo receiverAccount = subFlow(new AccountInfoByName(receiverName)).get(0)
                    .getState().getData();
            AnonymousParty receiverAnonymousParty = subFlow(new RequestKeyForAccount(receiverAccount));

            // Retrieve the input asset state for the transfer from your vault
            QueryCriteria myAccountQuery = new QueryCriteria.VaultQueryCriteria()
                    .withStateRefs(ImmutableList.of(assetRef));
            StateAndRef<AssetForAccountState> inputAsset = getServiceHub().getVaultService()
                    .queryBy(AssetForAccountState.class, myAccountQuery).getStates().get(0);
            AssetForAccountState inputAssetForAccountState = inputAsset.getState().getData();


            // Create the output state (the evolution of the input state)
            AssetForAccountState outputAssetForAccountState = new AssetForAccountState(inputAssetForAccountState.getIssuer(), receiverAnonymousParty, inputAssetForAccountState.getName(), inputAssetForAccountState.getValue());

            // Build the transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addCommand(new AssetForAccountContract.Commands.Transfer(), receiverAnonymousParty.getOwningKey())
                    .addInputState(inputAsset)
                    .addOutputState(outputAssetForAccountState);

            // Verify the transaction locally
            txBuilder.verify(getServiceHub());

            FlowSession receiverHostSession = initiateFlow(receiverAccount.getHost());
            // Sign the transaction
            SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(txBuilder, ImmutableList.of(getOurIdentity().getOwningKey()));

            // Collect the signatures
            List<? extends TransactionSignature> receiverSignature = subFlow(new CollectSignatureFlow(partiallySignedTx, receiverHostSession, receiverAnonymousParty.getOwningKey()));
            SignedTransaction signedByCounterParty = partiallySignedTx.withAdditionalSignature(receiverSignature.get(0));


            return subFlow(new FinalityFlow(signedByCounterParty, ImmutableList.of(receiverHostSession)));
        }
    }


    @InitiatedBy(NodeAccountToNodeAccountInitiatorFlow.class)
    public static class nodeAccountToNodeAccountResponderFlow extends FlowLogic<Void> {

        private final FlowSession counterParty;

        public nodeAccountToNodeAccountResponderFlow(FlowSession counterParty) {
            this.counterParty = counterParty;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            AtomicReference<AccountInfo> receivingAccountRef = new AtomicReference<>();

            SignedTransaction stx = subFlow(new SignTransactionFlow(counterParty) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    AnonymousParty keyOfReceiver = stx.getCoreTransaction().outRefsOfType(AssetForAccountState.class).get(0)
                            .getState().getData().getOwner();
                    if (keyOfReceiver != null) {
                        receivingAccountRef.set(subFlow(new AccountInfoByKey(keyOfReceiver.getOwningKey()))
                                .getState().getData());
                    }
                    if (receivingAccountRef.get() == null) {
                        throw new FlowException("Account to receive state was not found on this node");
                    }
                }
            });

            // record and finalize the transaction
            SignedTransaction receivedTx = subFlow(new ReceiveFinalityFlow(counterParty, stx.getId()));
            return null;
        }
    }
}
