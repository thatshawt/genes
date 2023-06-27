package xyz.davidpineiro.genes.core.evolution.std;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.Gene;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.GenomeFactory;

public class CharGenome extends Genome<CharGenome.CharGene> {

    public static class CharGene implements Gene {

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

        public static CharGene getRandom(){
            CharGene gene = new CharGene('a');
            gene.mutate();
            return gene;
        }

        @Override
        public String
        toString() {
            return "CharGene{" +
                    "value=" + value +
                    '}';
        }
    }


    @Override
    public Genome<CharGene> crossover(Genome<CharGene> other) {

        /*
        geneA: 32123
        geneB: 23123123123
        we cut right^here
         */

        Genome<CharGene> resultGenome = new CharGenome();

        final Genome<CharGene> longerGenome = other.size() >= this.size() ? other : this;
        final Genome<CharGene> smallerGenome = other.size() <= this.size() ? other : this;

        for(int i=0;i<smallerGenome.size();i++){
            if(Utils.chance(0.5f)) resultGenome.add(longerGenome.get(i));
            else resultGenome.add(smallerGenome.get(i));
        }
        for(int i=smallerGenome.size();i<longerGenome.size();i++){
            resultGenome.add(longerGenome.get(i));
        }

        return resultGenome;

    }

    public static Genome<CharGene> getRandomGenome() {
        return GenomeFactory.getRandomGenome(new CharGenome(), CharGene::getRandom, 10);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for(CharGene gene : this){
            builder.append(String.format("%s, ", gene.value));
        }
        builder.append("]");
        return builder.toString();
    }
}
