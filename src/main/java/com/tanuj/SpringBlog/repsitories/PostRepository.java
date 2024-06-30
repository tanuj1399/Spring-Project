package com.tanuj.SpringBlog.repsitories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tanuj.SpringBlog.models.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>{
    
}
