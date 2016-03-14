package com.gtranslator.storage.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
public class Translation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private Word sword;

    @ManyToOne(fetch = FetchType.EAGER)
    private Word tword;

    @ManyToOne(fetch = FetchType.EAGER)
    private Category category;

    @Column
    private BigDecimal weight;

    public Word getSword() {
        return sword;
    }

    public void setSword(Word word) {
        this.sword = word;
    }

    public Word getTword() {
        return tword;
    }

    public void setTword(Word tword) {
        this.tword = tword;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}