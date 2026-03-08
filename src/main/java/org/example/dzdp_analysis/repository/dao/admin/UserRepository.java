package org.example.dzdp_analysis.repository.dao.admin;
import org.example.dzdp_analysis.repository.entity.admin.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 修改查询方法，使用LIKE进行模糊匹配（处理编码问题）
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.username LIKE CONCAT('%', :username, '%')")
    List<User> findByUsernameWithEncoding(@Param("username") String username);

    // 保持原有的精确查询
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(User.UserRole role);

    List<User> findByStatus(User.UserStatus status);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<User> findByUsernameContainingOrEmailContaining(@Param("keyword") String keyword, Pageable pageable);

    Page<User> findByRole(User.UserRole role, Pageable pageable);

    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(@Param("role") User.UserRole role,
                                   @Param("status") User.UserStatus status,
                                   Pageable pageable);
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    // 添加默认的排序查询方法
    Page<User> findAllByOrderByCreateTimeDesc(Pageable pageable);
}