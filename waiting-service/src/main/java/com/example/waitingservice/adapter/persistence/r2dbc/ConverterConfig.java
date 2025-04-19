package com.example.waitingservice.adapter.persistence.r2dbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class ConverterConfig {
    @Bean
    public R2dbcCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new UUIDToByteArrayConverter());
        converters.add(new ByteArrayToUUIDConverter());
        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
    }

    @WritingConverter
    public class UUIDToByteArrayConverter implements Converter<UUID, byte[]> {
        @Override
        public byte[] convert(UUID source) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(source.getMostSignificantBits());
            bb.putLong(source.getLeastSignificantBits());
            return bb.array();
        }
    }

    @ReadingConverter
    public class ByteArrayToUUIDConverter implements Converter<byte[], UUID> {
        @Override
        public UUID convert(byte[] source) {
            if (source == null || source.length != 16) {
                throw new IllegalArgumentException("byte array must be of length 16");
            }

            long mostSigBits = 0;
            long leastSigBits = 0;

            for (int i = 0; i < 8; i++) {
                mostSigBits = (mostSigBits << 8) | (source[i] & 0xFF);
            }

            for (int i = 8; i < 16; i++) {
                leastSigBits = (leastSigBits << 8) | (source[i] & 0xFF);
            }

            return new UUID(mostSigBits, leastSigBits);
        }
    }
}
