package xyz.davidpineiro.genes.core.evolution.std;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.Gene;

public class CharGene implements Gene {

    private char value;

    public CharGene(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public void mutate() {
        value = Utils.getRandomPrintableChar();
    }

}
