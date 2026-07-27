package repository;

public interface DataRepository<T> {
    T load();

    void save(T data);
}
