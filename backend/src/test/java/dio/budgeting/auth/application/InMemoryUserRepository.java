package dio.budgeting.auth.application;


import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.auth.domain.UserRepository;

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
