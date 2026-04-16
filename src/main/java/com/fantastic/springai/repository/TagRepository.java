package com.fantastic.springai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fantastic.springai.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    Optional<Tag> findByNameIgnoreCase(String name);
}
