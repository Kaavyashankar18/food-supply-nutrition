package com.foodsupply.beneficiary.repository;

import com.foodsupply.beneficiary.entity.Beneficiary;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    @Query("""
            select b from Beneficiary b
            where (:q is null or lower(b.fullName) like lower(concat('%', cast(:q as string), '%'))
                   or lower(b.address) like lower(concat('%', cast(:q as string), '%')))
              and (:category is null or b.category = :category)
              and (:type is null or b.type = :type)
              and (:status is null or b.status = :status)
              and (:eligible is null or b.eligible = :eligible)
            """)
    Page<Beneficiary> search(@Param("q") String q,
                             @Param("category") BeneficiaryCategory category,
                             @Param("type") BeneficiaryType type,
                             @Param("status") BeneficiaryStatus status,
                             @Param("eligible") Boolean eligible,
                             Pageable pageable);

    List<Beneficiary> findByIdIn(List<Long> ids);

    boolean existsByType(BeneficiaryType type);

    long countByStatus(BeneficiaryStatus status);

    long countByEligibleTrueAndStatus(BeneficiaryStatus status);

    /** People covered (not records): a school of 500 counts as 500. */
    @Query("""
            select coalesce(sum(b.peopleServed), 0) from Beneficiary b
            where b.status = com.foodsupply.beneficiary.entity.BeneficiaryStatus.ACTIVE and b.eligible = true
            """)
    long totalPeopleServed();

    @Query("""
            select b.category, sum(b.peopleServed) from Beneficiary b
            where b.status = com.foodsupply.beneficiary.entity.BeneficiaryStatus.ACTIVE and b.eligible = true
            group by b.category
            """)
    List<Object[]> countPeopleByCategory();
}
