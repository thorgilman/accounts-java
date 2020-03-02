package com.bootcamp.contracts;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(AssetForAccountContract.class)
public class AssetForAccountState implements ContractState {

    // Define your properties and parties here
    private final AnonymousParty issuer;
    private final AnonymousParty owner;
    private final String name;
    private final long value;

    // Constructor for instantiating the state
    public AssetForAccountState(AnonymousParty issuer, AnonymousParty owner, String name, long value) {
        this.issuer = issuer;
        this.owner = owner;
        this.name = name;
        this.value = value;
    }

    // Getters for accessing properties of the state
    public AnonymousParty getIssuer() {
        return issuer;
    }

    public AnonymousParty getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return value;
    }

    // Participants are RELEVANT to the transaction and will receive
    // ledger updates for any creation, modification, deletion of the state
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }
}