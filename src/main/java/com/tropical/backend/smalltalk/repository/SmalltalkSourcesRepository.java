package com.tropical.backend.smalltalk.repository;

import com.tropical.backend.smalltalk.entity.SmalltalkSources;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmalltalkSourcesRepository extends JpaRepository<SmalltalkSources, Long> {

}
