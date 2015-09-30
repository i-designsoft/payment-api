package io.yope.payment.blockchain.bitcoinj;

import io.yope.payment.blockchain.BlockchainException;
import io.yope.payment.blockchain.BlockChainService;
import io.yope.payment.domain.Transaction;
import io.yope.payment.domain.Wallet;
import io.yope.payment.mock.domain.WalletMock;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.UnreadableWalletException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Bitcoinj client implementation.
 */
@Slf4j
@AllArgsConstructor
public class BitcoinjBlockchainServiceImpl implements BlockChainService {

    NetworkParameters params;

    BlockChain chain;

    PeerGroup peerGroup;

    public void init(List<Wallet> wallets) {
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        peerGroup.setBloomFilterFalsePositiveRate(0.00001);

        for (Wallet wallet : wallets) {
            try {
                registerInBlockchain(org.bitcoinj.core.Wallet.loadFromFileStream(
                        new ByteArrayInputStream(wallet.getContent())));
            } catch (UnreadableWalletException e) {
                log.error("wallet {} cannot be registered to the chain", wallet.getHash());
            }
        }
        peerGroup.startAsync();
        peerGroup.downloadBlockChain();
    }

    @Override
    public Wallet register() throws BlockchainException {
        org.bitcoinj.core.Wallet btcjWallet = new org.bitcoinj.core.Wallet(params);
        DeterministicKey freshKey = btcjWallet.freshReceiveKey();
        //todo: how many confirmations does Yope wait?
        btcjWallet.allowSpendingUnconfirmedTransactions();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            btcjWallet.saveToFileStream(outputStream);
        } catch (IOException e) {
            throw new BlockchainException(e);
        }
        registerInBlockchain(btcjWallet);
        WalletMock wallet = new WalletMock();
        wallet.hash(freshKey.toAddress(params).toString()).
                content(outputStream.toByteArray()).
                publicKey(freshKey.getPrivateKeyEncoded(params).toString());
        return wallet;
    }

    @Override
    public void send(Transaction transaction) throws BlockchainException {
        try {
            Coin value = Coin.valueOf(transaction.getAmount().longValue());
            org.bitcoinj.core.Wallet sender =
                    org.bitcoinj.core.Wallet.loadFromFileStream(
                            new ByteArrayInputStream(transaction.getSource().getContent()));
            sender.allowSpendingUnconfirmedTransactions();
            registerInBlockchain(sender);
            Address receiver = new Address(params, transaction.getDestination().getHash());
            sender.sendCoins(peerGroup, receiver, value);
        } catch (UnreadableWalletException e) {
            throw new BlockchainException(e);
        } catch (InsufficientMoneyException e) {
            throw new BlockchainException(e);
        } catch (AddressFormatException e) {
            throw new BlockchainException(e);
        }
    }

    private void registerInBlockchain(org.bitcoinj.core.Wallet wallet) {
        chain.addWallet(wallet);
        peerGroup.addWallet(wallet);
    }

}