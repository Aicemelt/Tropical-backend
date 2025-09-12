package com.tropical.backend.bucketList.repository;

import com.tropical.backend.bucketList.entity.BucketList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BucketListRepository extends JpaRepository<BucketList, Long> {

}
