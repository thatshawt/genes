package xyz.davidpineiro.genes.core.evolution.std;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.Gene;

public class CharGene extends Gene {

    private char value;

    public CharGene(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    @Override
    protected void mutate() {
        value = Utils.getRandomPrintableChar();
    }
}
