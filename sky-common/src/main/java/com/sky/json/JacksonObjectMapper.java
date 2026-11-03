package com.sky.json;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class JacksonObjectMapper extends ObjectMapper {
    public JacksonObjectMapper() {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule numbers = new SimpleModule();
        numbers.addSerializer(Long.class, ToStringSerializer.instance);
        numbers.addSerializer(Long.TYPE, ToStringSerializer.instance);
        registerModule(numbers);
        JavaTimeModule time = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        time.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        time.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        registerModule(time);
    }
}
