package com.crewmeister.cmcodingchallenge.repository;

import com.crewmeister.cmcodingchallenge.model.ExchangeRateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

    List<ExchangeRateEntity> findByDate(String date);

    Optional<ExchangeRateEntity> findByCurrencyCodeAndDate(String currencyCode, String date);

    Page<ExchangeRateEntity> findAll(Pageable pageable);

    boolean existsByCurrencyCodeAndDate(String currencyCode, String date);

}

