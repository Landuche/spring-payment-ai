package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.User;
import dio.budgeting.domain.UserId;
import dio.budgeting.domain.UserRepository;
import dio.budgeting.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

interface UserEntityRepository extends CrudRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
}

@Repository
class UserJpaRepository implements UserRepository {
    private final UserEntityRepository entityRepository;

    UserJpaRepository(UserEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public User save(User user) {
        return entityRepository.save(UserEntity.from(user)).toDomain();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return entityRepository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return entityRepository.findById(id.uuid()).map(UserEntity::toDomain);
    }
}
