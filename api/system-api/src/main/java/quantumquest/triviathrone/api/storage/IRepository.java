package quantumquest.triviathrone.api.storage;

import java.util.List;

public interface IRepository<TEntity> {
    List<TEntity> findAll();

    void insertAll(List<TEntity> entities);
}
