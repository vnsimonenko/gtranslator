package com.gtranslator.storage.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import scala.Equals;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"text", "lang"})},
        indexes = {@Index(name = "text_lang_index", columnList = "text, lang")
        })
@Entity
@NamedEntityGraph(name = "Word.dependencies",
        attributeNodes = {@NamedAttributeNode("translations"), @NamedAttributeNode("transcriptions")})

public class Word implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sword", cascade = {CascadeType.REFRESH})
    private Set<Translation> translations;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "word", cascade = {CascadeType.REFRESH})
    Set<Transcription> transcriptions;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Lang lang;

    @Column
    private long status;

    public Set<Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(Set<Translation> translations) {
        this.translations = translations;
    }

    public Set<Transcription> getTranscriptions() {
        return transcriptions;
    }

    public void setTranscriptions(Set<Transcription> transcriptions) {
        this.transcriptions = transcriptions;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Lang getLang() {
        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public long getStatus() {
        return status;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public boolean isNew() {
        return id == 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.asList(id, text, lang).toArray());
    }
}
