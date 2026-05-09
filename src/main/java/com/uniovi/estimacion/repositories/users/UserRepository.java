package com.uniovi.estimacion.repositories.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<User> findAllByOrderByUsernameAsc(Pageable pageable);

    List<User> findByRoleOrderByUsernameAsc(UserRole role);

    Page<User> findByRoleOrderByUsernameAsc(UserRole role, Pageable pageable);

    List<User> findByProjectManagerIdOrderByUsernameAsc(Long projectManagerId);

    Page<User> findByProjectManagerIdOrderByUsernameAsc(Long projectManagerId, Pageable pageable);

    boolean existsByProjectManagerId(Long projectManagerId);

    List<User> findByRoleAndProjectManagerIdOrderByUsernameAsc(UserRole role, Long projectManagerId);

    Page<User> findByRoleAndProjectManagerIdOrderByUsernameAsc(UserRole role, Long projectManagerId, Pageable pageable);

    Optional<User> findByIdAndProjectManagerId(Long userId, Long projectManagerId);
}