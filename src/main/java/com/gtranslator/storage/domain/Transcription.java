package com.gtranslator.storage.domain;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class Transcription implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private Word word;

    @Column(nullable = false, unique = false)
    private String text;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Phonetic phonetic;

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Phonetic getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(Phonetic phonetic) {
        this.phonetic = phonetic;
    }
}
