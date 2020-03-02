package com.bootcamp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class ShareAccountToParty extends FlowLogic<Void> {

    private final String acctName;
    private final Party shareTo;

    public ShareAccountToParty(String acctName, Party shareTo) {
        this.acctName = acctName;
        this.shareTo = shareTo;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {

        StateAndRef<AccountInfo> account = (StateAndRef<AccountInfo>) subFlow(new AccountInfoByName(acctName)).get(0);
        subFlow(new ShareAccountInfo(account, ImmutableList.of(shareTo)));

        return null;
    }
}
