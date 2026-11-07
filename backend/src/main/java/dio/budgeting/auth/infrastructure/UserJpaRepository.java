package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.auth.domain.UserRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

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
