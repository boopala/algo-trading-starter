package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, String> {

}
