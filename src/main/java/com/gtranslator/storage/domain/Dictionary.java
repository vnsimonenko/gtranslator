package com.gtranslator.storage.domain;

import com.gtranslator.storage.converter.ZipConverter;
import com.gtranslator.storage.validator.DictionaryValidator;
import com.gtranslator.storage.validator.ValidatedBy;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.validation.annotation.Validated;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@EntityListeners({AuditingEntityListener.class})
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"source", "lang"})},
        indexes = {@Index(name = "source_lang_index", columnList = "source, lang")
        })
@Validated
public class Dictionary implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    @CreatedBy
    @LastModifiedBy
    private String source;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @CreatedBy
    @LastModifiedBy
    private Lang lang;

    @Column(nullable = true, length = Integer.MAX_VALUE)
    @Convert(converter = ZipConverter.class)
    @ValidatedBy(DictionaryValidator.class)
    @CreatedBy
    @LastModifiedBy
    private String data;

    @LastModifiedDate
    @CreatedDate
    //@Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Lang getLang() {
        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public boolean isNew() {
        return id == 0;
    }

    @Override
    public String toString() {
        return "Dictionary{" +
                "id=" + id +
                ", source='" + source + '\'' +
                ", lang=" + lang +
                ", data='" + data + '\'' +
                ", lastModifiedDate=" + lastModifiedDate +
                '}';
    }
}
