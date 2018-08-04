import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        HashSet<UTXO> spentUtxos = new HashSet<>();
        double inputTotal = 0;
        double outputTotal = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            // rule1
            Transaction.Input input = tx.getInput(i);
            if (null == input) return false;

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!this.utxoPool.contains(utxo)) return false;

            // rule2
            Transaction.Output output = this.utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) return false;

            // rule3
            if (spentUtxos.contains(utxo)) return false;

            spentUtxos.add(utxo);
            inputTotal += output.value;
        }

        for (int i = 0; i < tx.numOutputs(); i++) {
            // rule4
            Transaction.Output output = tx.getOutput(i);

            if (output.value < 0) return false;
            outputTotal += output.value;
        }

        // rule5
        return inputTotal >= outputTotal;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        ArrayList<Transaction> validTxs = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);

                for (Transaction.Input input : tx.getInputs()) {
                    this.utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }

                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    this.utxoPool.addUTXO(new UTXO(tx.getHash(), i), output);
                }
            }
        }

        return validTxs.toArray(new Transaction[0]);
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }
}
