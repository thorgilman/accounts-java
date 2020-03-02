package com.bootcamp.flows;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        ImmutableList.of(
                                TestCordapp.findCordapp("com.bootcamp.contracts"),
                                TestCordapp.findCordapp("com.bootcamp.flows"),
                                TestCordapp.findCordapp("com.r3.corda.lib.ci"),
                                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows")
                        )
                ));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        network.runNetwork();
        nodeA.startFlow(new CreateAccount("BobAcct"));
        nodeB.startFlow(new CreateAccount("AliceAcct"));
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    // Issue Flow Tests
    @Test
    public void nodeToNodeAccountTx() throws Exception {
        AssetForAccountStateSelfIssueFlow flow = new AssetForAccountStateSelfIssueFlow("BobAcct", "diamond", 10);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction issueTx = future.get();
        System.out.println(issueTx);
        
        // get AccountInfo for AliceAcct
        CordaFuture<List<? extends StateAndRef<? extends AccountInfo>>> future3 = nodeB.startFlow(new AccountInfoByName("AliceAcct"));
        network.runNetwork();

        StateAndRef<AccountInfo> aliceAcctInfo = (StateAndRef<AccountInfo>) future3.get().get(0);

        // share Alice with nodeA
        nodeB.startFlow(new ShareAccountToParty("AliceAcct", nodeA.getInfo().getLegalIdentities().get(0)));
        //nodeB.startFlow(new ShareAccountInfo(aliceAcctInfo, ImmutableList.of(nodeA.getInfo().getLegalIdentities().get(0))));
        network.runNetwork();
        
        NodeAccountToNodeAccountFlows.NodeAccountToNodeAccountInitiatorFlow flow2 = new NodeAccountToNodeAccountFlows.NodeAccountToNodeAccountInitiatorFlow(
                "BobAcct", "AliceAcct", issueTx.getId().toString(), 0
        );
        CordaFuture<SignedTransaction> future2 = nodeA.startFlow(flow2);
        network.runNetwork();
        SignedTransaction moveTx = future2.get();
        System.out.println(moveTx);


        assertEquals(1, moveTx.getTx().getOutputStates().size());
//      TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//       assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
    }

}