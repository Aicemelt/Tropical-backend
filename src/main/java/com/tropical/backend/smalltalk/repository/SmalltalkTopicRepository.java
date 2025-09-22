package com.tropical.backend.smalltalk.repository;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.bucketList.entity.BucketList;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmalltalkTopicRepository extends JpaRepository<SmalltalkTopic, Long>, SmalltalkTopicCustom{

}