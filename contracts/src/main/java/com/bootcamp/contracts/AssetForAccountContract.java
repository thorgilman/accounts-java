package com.bootcamp.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class AssetForAccountContract implements Contract {
    public static String ID = "com.bootcamp.contracts.AssetForAccountContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        /**
         * Put your checks here. For purpose of demonstrating Accounts we will
         * not put in constraints. Every transaction will verify.
         */
    }

    public interface Commands extends CommandData {
        class Transfer implements Commands {}
    }
}
