package xyz.davidpineiro.genes.core.problems;

public interface Problem<T> {
    default T resolve(){
        return (T)this;
    }
}
