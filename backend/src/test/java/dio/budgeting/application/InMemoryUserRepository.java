package dio.budgeting.application;

import dio.budgeting.domain.User;
import dio.budgeting.domain.UserId;
import dio.budgeting.domain.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {
    private final List<User> store = new ArrayList<>();

    @Override
    public User save(User user) {
        store.add(user);
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return store.stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return store.stream().filter(u -> u.id().equals(id)).findFirst();
    }
}
