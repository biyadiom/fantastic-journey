package com.fantastic.springai.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "countries", schema = "shop")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 2)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @OneToMany(mappedBy = "country")
    @JsonIgnore
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "country")
    @JsonIgnore
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();
}
