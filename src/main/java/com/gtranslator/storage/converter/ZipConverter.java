package com.gtranslator.storage.converter;

import com.gtranslator.BaseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Converter
public class ZipConverter implements AttributeConverter<String, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(String rawStr) {
        if (StringUtils.isBlank(rawStr)) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream z = new GZIPOutputStream(out)) {
            z.write(rawStr.getBytes("UTF-8"));
        } catch (IOException ex) {
            throw new BaseException(ex);
        }
        return out.toByteArray();
    }

    @Override
    public String convertToEntityAttribute(byte[] blob) {
        if (blob == null || blob.length == 0) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(blob);
        try (GZIPInputStream z = new GZIPInputStream(in)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(z, out);
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException ex) {
            throw new BaseException(ex);
        }
    }
}
