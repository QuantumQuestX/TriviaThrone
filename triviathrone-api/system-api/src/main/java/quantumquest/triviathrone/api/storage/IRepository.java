package quantumquest.triviathrone.api.storage;

import java.util.List;

/**
 * Basic persistence contract for entities of one type.
 *
 * @param <TEntity> entity type managed by this repository
 * @author TiJ
 * @since 2.0.0
 */
public interface IRepository<TEntity> {
    /**
     * Returns all persisted entities in repository-defined order.
     */
    List<TEntity> findAll();

    /**
     * Persists every supplied entity as one repository operation.
     */
    void insertAll(List<TEntity> entities);
}
