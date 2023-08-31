package xyz.davidpineiro.genes.core.evolution;

public abstract class AbstractGene implements IGene {

    protected boolean active = true;

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void flipActive() {
        active = !active;
    }

    public abstract IGene clone();
}
