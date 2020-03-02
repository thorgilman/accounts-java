package com.bootcamp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.bootcamp.contracts.AssetForAccountContract;
import com.bootcamp.contracts.AssetForAccountState;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Collections;

@StartableByRPC
public class AssetForAccountStateSelfIssueFlow extends FlowLogic<SignedTransaction> {

    // Define your properties and parties here
    private final String acctName;
    private final String name;
    private final long value;


    public AssetForAccountStateSelfIssueFlow(String acctName, String name, long value) {
        this.acctName = acctName;
        this.name = name;
        this.value = value;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // NOTE NOTE NOTE
        // This is a node level self-issuance so using account name as identifier is sufficient
        // HOWEVER at the network level you can not enforce uniqueness on the acctName alone.
        // user either i) tuple of acctName and host, OR ii) UUID

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        AccountInfo issuerOwner = subFlow(new AccountInfoByName(acctName)).get(0).getState().getData();
        PublicKey issuerOwnerKey = getServiceHub().getKeyManagementService().freshKey(issuerOwner.getIdentifier().getId());
        AnonymousParty issuerOwnerAnonParty = new AnonymousParty(issuerOwnerKey);

        // issuer ALSO the owner
        AssetForAccountState outputAsset = new AssetForAccountState(issuerOwnerAnonParty, issuerOwnerAnonParty, name, value);

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputAsset, AssetForAccountContract.ID)
                .addCommand(new AssetForAccountContract.Commands.Transfer(), issuerOwnerKey);
        
        txBuilder.verify(getServiceHub());

        SignedTransaction fullySignedTransaction = getServiceHub().signInitialTransaction(txBuilder, issuerOwnerKey);

        return subFlow(new FinalityFlow(fullySignedTransaction, Collections.emptyList()));
    }
}
