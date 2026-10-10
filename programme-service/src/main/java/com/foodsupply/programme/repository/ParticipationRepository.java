package com.foodsupply.programme.repository;

import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ParticipationStatus;
import com.foodsupply.programme.entity.ProgrammeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    List<Participation> findByProgrammeIdOrderByIdAsc(Long programmeId);

    Optional<Participation> findByProgrammeIdAndBeneficiaryId(Long programmeId, Long beneficiaryId);

    Optional<Participation> findByIdAndProgrammeId(Long id, Long programmeId);

    @Query("select count(p) from Participation p where p.status = :s and p.programme.status = :ps")
    long countByStatusInProgrammes(@Param("s") ParticipationStatus s, @Param("ps") ProgrammeStatus ps);

    @Query("select coalesce(sum(p.peopleCovered), 0L) from Participation p where p.status = :s and p.programme.status = :ps")
    Number sumPeopleByStatusInProgrammes(@Param("s") ParticipationStatus s, @Param("ps") ProgrammeStatus ps);
}
