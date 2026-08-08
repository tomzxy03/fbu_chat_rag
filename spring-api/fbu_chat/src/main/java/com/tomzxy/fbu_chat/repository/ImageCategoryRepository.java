package com.tomzxy.fbu_chat.repository;

import com.tomzxy.fbu_chat.entity.ImageCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageCategoryRepository extends JpaRepository<ImageCategory, Integer> {

    /** Tất cả category đang active, sắp xếp theo sort_order — dùng cho dropdown UI */
    List<ImageCategory> findByActiveTrueOrderBySortOrderAsc();

    Optional<ImageCategory> findByCode(String code);

    boolean existsByCode(String code);
}
