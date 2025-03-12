package com.crewmeister.cmcodingchallenge.model;

import lombok.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CurrencyEntity {

    @Id
    @Column(name = "code", length = 3, nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

}
